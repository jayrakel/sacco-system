package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.SystemSettingService;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.*;
import com.sacco.sacco_system.modules.loan.domain.repository.*;
import com.sacco.sacco_system.modules.member.domain.entity.EmploymentDetails;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
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
public class LoanOriginationService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final LoanProductRepository loanProductRepository;
    private final GuarantorRepository guarantorRepository;

    private final AccountingService accountingService;
    private final TransactionRepository transactionRepository;
    private final LoanLimitService loanLimitService;
    private final RepaymentScheduleService repaymentScheduleService;
    private final SystemSettingService systemSettingService;
    private final NotificationService notificationService;
    private final ReferenceCodeService referenceCodeService;

    // --- ELIGIBILITY ---
    public Map<String, Object> checkEligibility(UUID userId) {
        Member member = memberRepository.findByUserId(userId).orElse(null);
        if (member == null) return Map.of("eligible", false, "reasons", List.of("Member not found"));

        Map<String, Object> limitDetails = loanLimitService.calculateMemberLoanLimitWithDetails(member);

        return Map.of("eligible", true, "details", limitDetails);
    }

    public List<Member> getEligibleGuarantors(UUID applicantUserId) {
        Member applicant = memberRepository.findByUserId(applicantUserId).orElseThrow();
        return memberRepository.findByStatus(Member.MemberStatus.ACTIVE).stream()
                .filter(m -> !m.getId().equals(applicant.getId()))
                .collect(Collectors.toList());
    }

    // --- INITIATION ---
    public LoanDTO initiateWithFee(UUID userId, UUID productId, String userExtRef, String method) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        LoanProduct product = loanProductRepository.findById(productId).orElseThrow();
        BigDecimal fee = product.getProcessingFee() != null ? product.getProcessingFee() : BigDecimal.ZERO;

        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            String ref = referenceCodeService.generateReferenceCode();
            accountingService.postEvent("LOAN_PROCESSING_FEE", "App Fee", ref, fee, "1002", null);

            transactionRepository.save(Transaction.builder()
                    .member(member).amount(fee).type(Transaction.TransactionType.PROCESSING_FEE)
                    .paymentMethod(Transaction.PaymentMethod.MPESA).referenceCode(ref)
                    .transactionDate(LocalDateTime.now()).build());
        }

        Loan loan = Loan.builder()
                .loanNumber("LN-" + System.currentTimeMillis())
                .member(member).product(product).status(Loan.LoanStatus.DRAFT)
                .applicationDate(LocalDate.now()).applicationFeePaid(true).build();
        return LoanDTO.fromEntity(loanRepository.save(loan));
    }

    public LoanDTO submitApplication(UUID loanId, BigDecimal amount, Integer duration) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();

        // 1. Check Limits
        if (!loanLimitService.canMemberBorrow(loan.getMember(), amount)) {
            throw new RuntimeException("Amount exceeds available limit.");
        }

        // 2. Set Default Unit to WEEKS (As requested)
        Loan.DurationUnit unit = Loan.DurationUnit.WEEKS;
        loan.setDurationUnit(unit);

        // 3. Calculate Repayment (Passing WEEKS unit)
        BigDecimal weeklyRepayment = repaymentScheduleService.calculateWeeklyRepayment(
                amount,
                loan.getProduct().getInterestRate(),
                duration,
                unit
        );

        // 4. Validate Ability to Pay
        validateAbilityToPay(loan.getMember(), weeklyRepayment);

        loan.setPrincipalAmount(amount);
        loan.setDuration(duration);
        loan.setWeeklyRepaymentAmount(weeklyRepayment);
        loan.setInterestRate(loan.getProduct().getInterestRate());
        loan.setStatus(Loan.LoanStatus.GUARANTORS_PENDING);

        return LoanDTO.fromEntity(loanRepository.save(loan));
    }

    // --- GUARANTORS ---
    public GuarantorDTO addGuarantor(UUID loanId, UUID guarantorId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        Member guarantor = memberRepository.findById(guarantorId).orElseThrow();

        if (guarantor.getId().equals(loan.getMember().getId())) throw new RuntimeException("Cannot guarantee self");

        Guarantor g = Guarantor.builder().loan(loan).member(guarantor).guaranteeAmount(amount)
                .status(Guarantor.GuarantorStatus.PENDING).dateRequestSent(LocalDate.now()).build();

        g = guarantorRepository.save(g);
        notificationService.notifyUser(guarantorId, "Guarantorship Request",
                "Please guarantee loan " + loan.getLoanNumber(), true, false);
        return GuarantorDTO.fromEntity(g);
    }

    public void finalizeGuarantorRequests(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        boolean sent = false;
        for(Guarantor g : loan.getGuarantors()) {
            if(g.getStatus() == Guarantor.GuarantorStatus.PENDING) {
                notificationService.notifyUser(g.getMember().getId(), "Reminder", "Action Required: Guarantorship", true, false);
                sent = true;
            }
        }
        if(sent) notificationService.notifyUser(loan.getMember().getId(), "Requests Sent", "Guarantor requests sent/resent.", true, false);
    }

    public void respondToGuarantorRequest(UUID userId, UUID requestId, String statusStr) {
        Guarantor req = guarantorRepository.findById(requestId).orElseThrow();
        if(!req.getMember().getUser().getId().equals(userId)) throw new RuntimeException("Unauthorized");

        req.setStatus(Guarantor.GuarantorStatus.valueOf(statusStr.toUpperCase()));
        guarantorRepository.save(req);

        if (req.getLoan().getGuarantors().stream().allMatch(g -> g.getStatus() == Guarantor.GuarantorStatus.ACCEPTED)) {
            req.getLoan().setStatus(Loan.LoanStatus.SUBMITTED);
            req.getLoan().setSubmissionDate(LocalDate.now());
            loanRepository.save(req.getLoan());
            notificationService.notifyUser(req.getLoan().getMember().getId(), "Loan Submitted", "All guarantors accepted.", true, true);
        }
    }

    private void validateAbilityToPay(Member member, BigDecimal weeklyRepayment) {
        EmploymentDetails emp = member.getEmploymentDetails();
        if (emp != null && emp.getNetMonthlyIncome() != null) {
            BigDecimal monthly = weeklyRepayment.multiply(BigDecimal.valueOf(4.33));
            double maxRatio = systemSettingService.getDouble("MAX_DEBT_RATIO", 0.66);
            if (monthly.compareTo(emp.getNetMonthlyIncome().multiply(BigDecimal.valueOf(maxRatio))) > 0) {
                throw new RuntimeException("Repayment too high for income.");
            }
        }
    }
}