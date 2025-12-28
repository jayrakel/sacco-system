package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
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
    private final LoanLimitService loanLimitService;
    private final RepaymentScheduleService repaymentScheduleService;
    private final SystemSettingService systemSettingService;
    private final NotificationService notificationService;

    /**
     * ✅ ELIGIBILITY CHECK
     * Fetches the keys you manually added via the Admin Dashboard.
     */
    public Map<String, Object> checkEligibility(UUID userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Member record not found"));

        // Match these exactly to what you type in the "Add Setting" modal
        int requiredMonths = Integer.parseInt(systemSettingService.getString("MINIMUM_MEMBERSHIP_MONTHS", "3"));
        BigDecimal requiredSavings = new BigDecimal(systemSettingService.getString("MINIMUM_LOAN_SAVINGS", "5000"));
        int maxActiveLoans = Integer.parseInt(systemSettingService.getString("MAX_ACTIVE_LOANS", "1"));

        Map<String, Object> response = new HashMap<>();
        List<String> reasons = new ArrayList<>();

        // Live Balance Sum (Always accurate)
        BigDecimal currentSavings = savingsAccountRepository.findByMemberId(member.getId())
                .stream()
                .map(SavingsAccount::getBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Active Loan Count
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

    /**
     * ✅ INITIATE LOAN (FEE FIRST)
     */
    public LoanDTO initiateWithFee(UUID userId, UUID productId, String referenceCode) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        LoanProduct product = loanProductRepository.findById(productId).orElseThrow();

        BigDecimal fee = product.getProcessingFee() != null ? product.getProcessingFee() : BigDecimal.ZERO;
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.postEvent("LOAN_PROCESSING_FEE", "App Fee - " + member.getMemberNumber(), referenceCode, fee);
        }

        Loan loan = Loan.builder()
                .loanNumber("LN-" + System.currentTimeMillis())
                .member(member)
                .product(product)
                .principalAmount(BigDecimal.ZERO)
                .status(Loan.LoanStatus.DRAFT)
                .applicationDate(LocalDate.now())
                .applicationFeePaid(true)
                .build();

        return convertToDTO(loanRepository.save(loan));
    }

    /**
     * ✅ SUBMIT APPLICATION
     */
    public LoanDTO submitApplication(UUID loanId, BigDecimal amount, Integer duration) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();

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

        loan.setPrincipalAmount(amount);
        loan.setDuration(duration);
        loan.setWeeklyRepaymentAmount(weeklyRepayment);
        loan.setStatus(Loan.LoanStatus.GUARANTORS_PENDING);

        return convertToDTO(loanRepository.save(loan));
    }

    /**
     * ✅ ADD GUARANTOR
     */
    public GuarantorDTO addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        Member guarantor = memberRepository.findById(guarantorMemberId).orElseThrow();

        if (guarantor.getId().equals(loan.getMember().getId())) {
            throw new RuntimeException("Conflict: You cannot guarantee your own loan.");
        }

        Guarantor g = Guarantor.builder()
                .loan(loan)
                .member(guarantor)
                .guaranteeAmount(amount)
                .status(Guarantor.GuarantorStatus.PENDING)
                .dateRequestSent(LocalDate.now())
                .build();

        notificationService.notifyUser(guarantor.getId(), "Guarantorship Request",
                "Action required for loan " + loan.getLoanNumber(), true, false);

        return convertToGuarantorDTO(guarantorRepository.save(g));
    }

    // --- QUERIES & HELPERS ---

    public List<LoanDTO> getLoansByMember(UUID userId) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        return loanRepository.findByMemberId(member.getId()).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public LoanDTO getLoanById(UUID loanId) {
        return loanRepository.findById(loanId)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
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
                .memberName(loan.getMember().getFirstName() + " " + loan.getMember().getLastName())
                .principalAmount(loan.getPrincipalAmount())
                .loanBalance(loan.getLoanBalance())
                .expectedRepaymentDate(loan.getExpectedRepaymentDate() != null ? loan.getExpectedRepaymentDate().toString() : null)
                .build();
    }

    private GuarantorDTO convertToGuarantorDTO(Guarantor g) {
        return GuarantorDTO.builder()
                .id(g.getId())
                .memberName(g.getMember().getFirstName() + " " + g.getMember().getLastName())
                .guaranteeAmount(g.getGuaranteeAmount())
                .status(g.getStatus().toString())
                .build();
    }
}