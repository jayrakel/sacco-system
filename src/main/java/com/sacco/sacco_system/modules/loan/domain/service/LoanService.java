package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.member.domain.entity.EmploymentDetails;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final LoanProductRepository loanProductRepository;
    private final GuarantorRepository guarantorRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final AccountingService accountingService;
    private final TransactionRepository transactionRepository;
    private final LoanLimitService loanLimitService;
    private final RepaymentScheduleService repaymentScheduleService;
    private final SystemSettingService systemSettingService;
    private final NotificationService notificationService;
    private final ReferenceCodeService referenceCodeService;

    // ... (checkEligibility and getEligibleGuarantors remain unchanged) ...
    public Map<String, Object> checkEligibility(UUID userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Member record not found"));

        int requiredMonths = Integer.parseInt(systemSettingService.getString("MIN_MONTHS_MEMBERSHIP", "3"));
        BigDecimal requiredSavings = new BigDecimal(systemSettingService.getString("MIN_SAVINGS_FOR_LOAN", "5000"));
        int maxActiveLoans = Integer.parseInt(systemSettingService.getString("MAX_ACTIVE_LOANS", "1"));

        Map<String, Object> response = new HashMap<>();
        List<String> reasons = new ArrayList<>();

        BigDecimal currentSavings = savingsAccountRepository.findByMember_Id(member.getId())
                .stream()
                .map(SavingsAccount::getBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeLoanCount = loanRepository.countByMemberIdAndStatusIn(
                member.getId(),
                List.of(Loan.LoanStatus.ACTIVE, Loan.LoanStatus.IN_ARREARS)
        );

        if (activeLoanCount >= maxActiveLoans) {
            reasons.add("You have reached the maximum of " + maxActiveLoans + " active loan(s).");
        }

        if (currentSavings.compareTo(requiredSavings) < 0) {
            reasons.add("Minimum savings of KES " + requiredSavings + " required.");
        }

        long monthsActive = 0;
        if (member.getCreatedAt() != null) {
            monthsActive = java.time.temporal.ChronoUnit.MONTHS.between(member.getCreatedAt(), LocalDateTime.now());
        }

        if (monthsActive < requiredMonths) {
            reasons.add("Minimum membership of " + requiredMonths + " month(s) required.");
        }

        response.put("eligible", reasons.isEmpty());
        response.put("reasons", reasons);
        response.put("currentSavings", currentSavings);
        response.put("requiredSavings", requiredSavings);
        response.put("membershipMonths", monthsActive);
        response.put("requiredMonths", requiredMonths);
        response.put("maxActiveLoans", maxActiveLoans);
        response.put("currentActiveLoans", activeLoanCount);

        return response;
    }

    public List<Member> getEligibleGuarantors(UUID applicantId) {
        BigDecimal minSavings = new BigDecimal(systemSettingService.getString("MIN_SAVINGS_TO_GUARANTEE", "10000"));
        int minMonths = Integer.parseInt(systemSettingService.getString("MIN_MONTHS_TO_GUARANTEE", "6"));

        List<Member> candidates = memberRepository.findByStatus(Member.MemberStatus.ACTIVE).stream()
                .filter(m -> !m.getId().equals(applicantId))
                .collect(Collectors.toList());

        List<Member> eligible = new ArrayList<>();

        for (Member m : candidates) {
            BigDecimal savings = savingsAccountRepository.findByMember_Id(m.getId()).stream()
                    .map(SavingsAccount::getBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (savings.compareTo(minSavings) < 0) continue;

            long monthsActive = 0;
            if (m.getCreatedAt() != null) {
                monthsActive = java.time.temporal.ChronoUnit.MONTHS.between(m.getCreatedAt(), LocalDate.now());
            }
            if (monthsActive < minMonths) continue;

            boolean hasDefaults = loanRepository.findByMemberId(m.getId()).stream()
                    .anyMatch(l -> l.getStatus() == Loan.LoanStatus.DEFAULTED || l.getStatus() == Loan.LoanStatus.WRITTEN_OFF);

            if (hasDefaults) continue;

            eligible.add(m);
        }

        return eligible;
    }

    public LoanDTO initiateWithFee(UUID userId, UUID productId, String userExternalReferenceCode, String paymentMethodStr) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        LoanProduct product = loanProductRepository.findById(productId).orElseThrow();

        BigDecimal fee = product.getProcessingFee() != null ? product.getProcessingFee() : BigDecimal.ZERO;

        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            // 1. Generate System Ref & Determine Payment Info
            String systemRef = referenceCodeService.generateReferenceCode();
            String sourceAccount = "1002"; // Default M-Pesa
            Transaction.PaymentMethod payMethod = Transaction.PaymentMethod.MPESA;

            if ("BANK".equalsIgnoreCase(paymentMethodStr)) {
                sourceAccount = "1010";
                payMethod = Transaction.PaymentMethod.BANK;
            } else if ("CASH".equalsIgnoreCase(paymentMethodStr)) {
                sourceAccount = "1001";
                payMethod = Transaction.PaymentMethod.CASH;
            }

            // 2. Post to General Ledger (Use System Ref)
            accountingService.postEvent(
                    "LOAN_PROCESSING_FEE",
                    "App Fee - " + member.getMemberNumber(),
                    systemRef,
                    fee,
                    sourceAccount,
                    null
            );

            // 3. ✅ FIX: Calculate Current Balance to preserve it
            BigDecimal currentTotalSavings = savingsAccountRepository.findByMember_Id(member.getId())
                    .stream()
                    .map(SavingsAccount::getBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 4. Create Transaction Record
            Transaction feeTransaction = Transaction.builder()
                    .member(member)
                    .amount(fee)
                    .type(Transaction.TransactionType.PROCESSING_FEE)
                    .paymentMethod(payMethod)
                    .referenceCode(systemRef) // System Code
                    .externalReference(userExternalReferenceCode) // User Code
                    .description("Loan App Fee: " + product.getName())
                    .balanceAfter(currentTotalSavings) // ✅ Preserves current balance!
                    .transactionDate(LocalDateTime.now())
                    .build();

            transactionRepository.save(feeTransaction);

            // 5. Notify Member
            notificationService.notifyUser(
                    member.getId(),
                    "Payment Received",
                    "Loan Fee Received. Receipt: " + systemRef,
                    true,
                    false
            );
        }

        Loan loan = Loan.builder()
                .loanNumber("LN-" + System.currentTimeMillis())
                .member(member)
                .product(product)
                .principalAmount(BigDecimal.ZERO)
                .status(Loan.LoanStatus.DRAFT)
                .applicationDate(LocalDate.now())
                .applicationFeePaid(true)
                .gracePeriodWeeks(0)
                .votingOpen(false)
                .votesYes(0)
                .votesNo(0)
                .build();

        return convertToDTO(loanRepository.save(loan));
    }

    // ... (rest of the file is identical to previous version) ...
    public LoanDTO submitApplication(UUID loanId, BigDecimal amount, Integer duration) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.isApplicationFeePaid()) {
            throw new RuntimeException("Application fee must be paid before submitting details.");
        }

        Map<String, Object> limitDetails = loanLimitService.calculateMemberLoanLimitWithDetails(loan.getMember());
        BigDecimal availableLimit = (BigDecimal) limitDetails.get("availableLimit");
        if (amount.compareTo(availableLimit) > 0)
            throw new RuntimeException("Exceeds limit of KES " + availableLimit);

        BigDecimal weeklyRepayment = repaymentScheduleService.calculateWeeklyRepayment(amount,
                loan.getProduct().getInterestRate(), duration, Loan.DurationUnit.MONTHS);

        validateAbilityToPay(loan.getMember(), weeklyRepayment);

        String gracePeriodStr = systemSettingService.getString("LOAN_GRACE_PERIOD_WEEKS", "0");
        try {
            loan.setGracePeriodWeeks(Integer.parseInt(gracePeriodStr));
        } catch (NumberFormatException e) {
            loan.setGracePeriodWeeks(0);
        }

        loan.setPrincipalAmount(amount);
        loan.setDuration(duration);
        loan.setWeeklyRepaymentAmount(weeklyRepayment);
        loan.setStatus(Loan.LoanStatus.GUARANTORS_PENDING);

        return convertToDTO(loanRepository.save(loan));
    }

    public GuarantorDTO addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        Member guarantor = memberRepository.findById(guarantorMemberId).orElseThrow(() -> new RuntimeException("Guarantor not found"));

        if (guarantor.getId().equals(loan.getMember().getId())) {
            throw new RuntimeException("Conflict: You cannot guarantee your own loan.");
        }

        boolean alreadyAdded = loan.getGuarantors().stream()
                .anyMatch(g -> g.getMember().getId().equals(guarantorMemberId));
        if (alreadyAdded) {
            throw new RuntimeException("This member is already a guarantor for this loan.");
        }

        Guarantor g = Guarantor.builder()
                .loan(loan)
                .member(guarantor)
                .guaranteeAmount(amount)
                .status(Guarantor.GuarantorStatus.PENDING)
                .dateRequestSent(LocalDate.now())
                .build();

        notificationService.notifyUser(guarantor.getId(), "Guarantorship Request",
                "Request from " + loan.getMemberName() + " for loan " + loan.getLoanNumber(), true, false);

        return convertToGuarantorDTO(guarantorRepository.save(g));
    }

    public void finalizeGuarantorRequests(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getGuarantors().isEmpty()) {
            throw new RuntimeException("At least one guarantor is required.");
        }

        loan.setStatus(Loan.LoanStatus.GUARANTORS_PENDING);
        loanRepository.save(loan);
    }

    public List<GuarantorDTO> getGuarantorsByLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        return guarantorRepository.findByLoan(loan).stream()
                .map(this::convertToGuarantorDTO)
                .collect(Collectors.toList());
    }

    public List<GuarantorDTO> getGuarantorRequests(UUID userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Member record not found"));

        return guarantorRepository.findByMemberAndStatus(member, Guarantor.GuarantorStatus.PENDING)
                .stream()
                .map(this::convertToGuarantorDTO)
                .collect(Collectors.toList());
    }

    public List<LoanDTO> getLoansByMember(UUID userId) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        return loanRepository.findByMemberId(member.getId()).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public LoanDTO getLoanById(UUID loanId) {
        return loanRepository.findById(loanId).map(this::convertToDTO).orElseThrow();
    }

    private void validateAbilityToPay(Member member, BigDecimal weeklyRepayment) {
        EmploymentDetails emp = member.getEmploymentDetails();
        if (emp != null && emp.getNetMonthlyIncome() != null) {
            BigDecimal monthlyRepayment = weeklyRepayment.multiply(BigDecimal.valueOf(4.33));
            String maxRatioStr = systemSettingService.getString("MAX_DEBT_RATIO", "0.66");
            double maxRatio = Double.parseDouble(maxRatioStr);
            if (monthlyRepayment.compareTo(emp.getNetMonthlyIncome().multiply(BigDecimal.valueOf(maxRatio))) > 0) {
                throw new RuntimeException("Repayment too high for income levels.");
            }
        }
    }

    private LoanDTO convertToDTO(Loan loan) {
        return LoanDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .status(loan.getStatus().toString())
                .memberName(loan.getMemberName())
                .principalAmount(loan.getPrincipalAmount())
                .loanBalance(loan.getLoanBalance())
                .expectedRepaymentDate(loan.getExpectedRepaymentDate() != null ? loan.getExpectedRepaymentDate().toString() : null)
                .build();
    }

    private GuarantorDTO convertToGuarantorDTO(Guarantor g) {
        return GuarantorDTO.builder()
                .id(g.getId())
                .memberId(g.getMember().getId())
                .loanId(g.getLoan().getId())
                .loanNumber(g.getLoan().getLoanNumber())
                .applicantName(g.getLoan().getMemberName())
                .memberName(g.getMember().getFirstName() + " " + g.getMember().getLastName())
                .guaranteeAmount(g.getGuaranteeAmount())
                .status(g.getStatus().toString())
                .build();
    }
}