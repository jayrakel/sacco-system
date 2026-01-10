package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.TransactionService;
import com.sacco.sacco_system.modules.loan.api.dto.LoanRequestDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanApplicationDraft;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanApplicationDraftRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.EmploymentDetails;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationService {

    private final LoanRepository loanRepository;
    private final LoanApplicationDraftRepository draftRepository;
    private final LoanProductRepository productRepository;
    private final GuarantorRepository guarantorRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final EmailService emailService; // ✅ Injected Email Service

    private final LoanEligibilityService eligibilityService;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final AccountingService accountingService;

    // --- READ CURRENT DRAFT ---
    public Optional<LoanApplicationDraft> getCurrentDraft(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member not found", 404));

        List<LoanApplicationDraft.DraftStatus> activeStatuses = Arrays.asList(
                LoanApplicationDraft.DraftStatus.PENDING_FEE,
                LoanApplicationDraft.DraftStatus.FEE_PAID
        );
        return draftRepository.findFirstByMemberIdAndStatusIn(member.getId(), activeStatuses);
    }

    // --- STEP 1: START DRAFT ---
    @Transactional
    public LoanApplicationDraft startApplication(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member not found", 404));

        if (!"ACTIVE".equals(member.getMemberStatus().name())) {
            throw new ApiException("Only active members can apply for loans", 400);
        }

        Map<String, Object> eligibility = eligibilityService.checkEligibility(email);
        if (!(boolean) eligibility.get("eligible")) {
            throw new ApiException("Cannot start application: " + eligibility.get("reasons"), 400);
        }

        return getCurrentDraft(email).orElseGet(() -> {
            log.info("Creating NEW Draft for {}", member.getMemberNumber());
            LoanApplicationDraft draft = LoanApplicationDraft.builder()
                    .member(member)
                    .draftReference("DRFT-" + (10000 + (long)(Math.random() * 90000)))
                    .feePaid(false)
                    .status(LoanApplicationDraft.DraftStatus.PENDING_FEE)
                    .build();
            return draftRepository.save(draft);
        });
    }

    // --- STEP 2: CONFIRM FEE ---
    @Transactional
    public LoanApplicationDraft confirmDraftFee(UUID draftId, String paymentReference) {
        LoanApplicationDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new ApiException("Draft application not found", 404));

        if (draft.isFeePaid()) return draft;

        boolean transactionExists = transactionRepository.findByExternalReference(paymentReference).isPresent();

        if (!transactionExists) {
            transactionService.recordProcessingFee(
                    draft.getMember(),
                    new BigDecimal("500"),
                    paymentReference,
                    "4000"
            );
        }

        draft.setFeePaid(true);
        draft.setStatus(LoanApplicationDraft.DraftStatus.FEE_PAID);
        return draftRepository.save(draft);
    }

    // --- STEP 3: CONVERT TO LOAN ---
    @Transactional
    public Loan createLoanFromDraft(UUID draftId, LoanRequestDTO request) {
        LoanApplicationDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new ApiException("Draft not found", 404));

        if (!draft.isFeePaid()) {
            throw new ApiException("Application fee not paid.", 400);
        }

        LoanProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ApiException("Product not found", 404));

        // GUARDRAIL 1: Product Limits
        if (request.getAmount().compareTo(product.getMinAmount()) < 0) {
            throw new ApiException("Amount is below minimum (" + product.getMinAmount() + ")", 400);
        }
        if (request.getAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new ApiException("Amount exceeds maximum (" + product.getMaxAmount() + ")", 400);
        }

        // GUARDRAIL 2: Duration
        if (request.getDurationWeeks() > product.getMaxDurationWeeks()) {
            throw new ApiException("Duration exceeds maximum (" + product.getMaxDurationWeeks() + " weeks)", 400);
        }

        // GUARDRAIL 3: Eligibility (3x Savings)
        BigDecimal memberLimit = eligibilityService.calculateMaxLoanLimit(draft.getMember());
        if (request.getAmount().compareTo(memberLimit) > 0) {
            throw new ApiException("Amount exceeds your eligible limit (3x Savings): KES " + memberLimit, 400);
        }

        // GUARDRAIL 4: Ability to Pay (Updated to allow Self-Guarantee Bypass)
        validateAbilityToPay(draft.getMember(), request.getAmount(), request.getDurationWeeks(), product);

        // GUARDRAIL 5: Liquidity
        BigDecimal lendableLiquidity = accountingService.calculateLendableLiquidity();
        if (request.getAmount().compareTo(lendableLiquidity) > 0) {
            log.warn("Liquidity Constraint. Req: {}, Lendable: {}", request.getAmount(), lendableLiquidity);
            throw new ApiException("Insufficient Sacco lendable funds. Please try a smaller amount.", 400);
        }

        String loanNumber = "LN-" + (100000 + (long)(Math.random() * 900000));

        Loan loan = Loan.builder()
                .member(draft.getMember())
                .product(product)
                .loanNumber(loanNumber)
                .principalAmount(request.getAmount())
                .interestRate(product.getInterestRate())
                .durationWeeks(request.getDurationWeeks())
                .loanStatus(Loan.LoanStatus.PENDING_GUARANTORS)
                .applicationDate(LocalDate.now())
                .feePaid(true)
                .totalOutstandingAmount(BigDecimal.ZERO)
                .build();

        Loan savedLoan = loanRepository.save(loan);

        draft.setStatus(LoanApplicationDraft.DraftStatus.CONVERTED);
        draftRepository.save(draft);

        return savedLoan;
    }

    /**
     * ✅ UPDATED: Ability to Pay Logic
     * 1. If Income Exists -> Apply 1/3 Rule.
     * 2. If NO Income -> Check if Loan <= Total Savings (Self-Guaranteed).
     */
    private void validateAbilityToPay(Member member, BigDecimal amount, int durationWeeks, LoanProduct product) {
        EmploymentDetails employment = member.getEmploymentDetails();
        BigDecimal netIncome = (employment != null) ? employment.getNetMonthlyIncome() : BigDecimal.ZERO;
        boolean hasIncome = netIncome != null && netIncome.compareTo(BigDecimal.ZERO) > 0;

        // --- PATH A: MEMBER HAS INCOME ---
        if (hasIncome) {
            BigDecimal months = BigDecimal.valueOf(durationWeeks).divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
            if (months.compareTo(BigDecimal.ZERO) == 0) months = BigDecimal.ONE;

            BigDecimal installment = calculateInstallment(amount, product.getInterestRate(), months, product.getInterestType());
            BigDecimal maxAllowable = netIncome.multiply(new BigDecimal("0.6667")); // 2/3rds Rule

            if (installment.compareTo(maxAllowable) > 0) {
                throw new ApiException(String.format(
                        "Monthly installment (KES %s) exceeds 2/3 of your income (KES %s). Increase duration or reduce amount.",
                        installment.setScale(2, RoundingMode.HALF_UP), maxAllowable.setScale(2, RoundingMode.HALF_UP)
                ), 400);
            }
            return; // Passed check
        }

        // --- PATH B: NO INCOME (Check Self-Guarantee) ---
        BigDecimal totalSavings = savingsAccountRepository.getTotalSavings(member.getId());
        if (totalSavings == null) totalSavings = BigDecimal.ZERO;

        // If Loan Amount is less than or equal to their savings, we allow it (Self-Secured)
        if (amount.compareTo(totalSavings) <= 0) {
            log.info("Ability to Pay Check Bypassed: Member {} has no income but loan is fully covered by savings.", member.getMemberNumber());
            return; // Passed check
        }

        // --- PATH C: FAIL ---
        throw new ApiException(
                "Ability to Pay Failed: You have no recorded employment income, and this loan amount (KES " + amount +
                        ") exceeds your total savings (KES " + totalSavings + "). Please update your employment profile or borrow within your savings limit.",
                400
        );
    }

    private BigDecimal calculateInstallment(BigDecimal amount, BigDecimal interestRate, BigDecimal months, LoanProduct.InterestType type) {
        BigDecimal rate = interestRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (type == LoanProduct.InterestType.FLAT) {
            BigDecimal totalInterest = amount.multiply(rate).multiply(months);
            return amount.add(totalInterest).divide(months, 2, RoundingMode.HALF_UP);
        } else {
            if (rate.doubleValue() == 0) return amount.divide(months, 2, RoundingMode.HALF_UP);
            double r = rate.doubleValue();
            double n = months.doubleValue();
            double numerator = r * Math.pow(1 + r, n);
            double denominator = Math.pow(1 + r, n) - 1;
            return amount.multiply(BigDecimal.valueOf(numerator / denominator));
        }
    }

    // --- STEP 4: ADD GUARANTORS (With Notifications) ---
    @Transactional
    public void addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new ApiException("Loan not found", 404));
        if (loan.getLoanStatus() != Loan.LoanStatus.PENDING_GUARANTORS) throw new ApiException("Invalid loan status", 400);

        Member guarantorMember = memberRepository.findById(guarantorMemberId).orElseThrow(() -> new ApiException("Guarantor not found", 404));

        if (!"ACTIVE".equals(guarantorMember.getMemberStatus().name())) throw new ApiException("Guarantor inactive", 400);
        if (loan.getMember().getId().equals(guarantorMember.getId())) throw new ApiException("Cannot self-guarantee here", 400);
        if (guarantorRepository.existsByLoanAndMember(loan, guarantorMember)) throw new ApiException("Already a guarantor", 400);

        BigDecimal totalSavings = savingsAccountRepository.getTotalSavings(guarantorMember.getId());
        BigDecimal ownLoans = loanRepository.getTotalOutstandingBalance(guarantorMember.getId());
        BigDecimal activeGuarantees = guarantorRepository.getTotalActiveLiability(guarantorMember.getId());

        if (totalSavings == null) totalSavings = BigDecimal.ZERO;
        if (ownLoans == null) ownLoans = BigDecimal.ZERO;
        if (activeGuarantees == null) activeGuarantees = BigDecimal.ZERO;

        BigDecimal freeMargin = totalSavings.subtract(ownLoans.add(activeGuarantees));

        if (freeMargin.compareTo(amount) < 0) {
            throw new ApiException("Guarantor has insufficient free deposits. Margin: " + freeMargin, 400);
        }

        Guarantor guarantor = Guarantor.builder()
                .loan(loan)
                .member(guarantorMember)
                .guaranteedAmount(amount)
                .status(Guarantor.GuarantorStatus.PENDING)
                .active(true)
                .build();
        guarantorRepository.save(guarantor);

        // ✅ SEND EMAIL TO GUARANTOR
        try {
            String subject = "Action Required: Guarantorship Request";
            String message = String.format(
                    "Hello %s,\n\n" +
                            "%s %s has requested you to guarantee their loan.\n" +
                            "Loan Amount: %s %s\n" +
                            "Requested Guarantee: %s %s\n\n" +
                            "Please log in to your dashboard to Approve or Reject this request.",
                    guarantorMember.getFirstName(),
                    loan.getMember().getFirstName(), loan.getMember().getLastName(),
                    loan.getCurrencyCode(), loan.getPrincipalAmount(),
                    loan.getCurrencyCode(), amount
            );
            emailService.sendEmail(guarantorMember.getEmail(), subject, message);
        } catch (Exception e) {
            log.error("Failed to send guarantor email", e);
        }
    }

    // --- STEP 5: GUARANTOR RESPONDS (Approve/Reject) ---
    @Transactional
    public void respondToGuarantorRequest(UUID guarantorId, boolean approved) {
        Guarantor guarantor = guarantorRepository.findById(guarantorId)
                .orElseThrow(() -> new ApiException("Request not found", 404));

        if (guarantor.getStatus() != Guarantor.GuarantorStatus.PENDING) {
            throw new ApiException("This request has already been processed.", 400);
        }

        // Update Status
        guarantor.setStatus(approved ? Guarantor.GuarantorStatus.ACCEPTED : Guarantor.GuarantorStatus.DECLINED);
        guarantorRepository.save(guarantor);

        Loan loan = guarantor.getLoan();
        Member applicant = loan.getMember();

        // Notify Applicant
        try {
            String statusText = approved ? "ACCEPTED" : "REJECTED";
            String subject = "Guarantor Response: " + statusText;
            String body = String.format(
                    "Hello %s,\n\n" +
                            "%s has %s your request to guarantee %s %s.\n\n" +
                            "Check your dashboard for details.",
                    applicant.getFirstName(),
                    guarantor.getMember().getFirstName(), statusText,
                    loan.getCurrencyCode(), guarantor.getGuaranteedAmount()
            );
            emailService.sendEmail(applicant.getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send applicant notification email", e);
        }

        // Check if all are done
        if (loan.getLoanStatus() == Loan.LoanStatus.AWAITING_GUARANTORS) {
            checkAndProgressLoan(loan);
        }
    }

    private void checkAndProgressLoan(Loan loan) {
        List<Guarantor> allGuarantors = guarantorRepository.findAllByLoan(loan);

        boolean anyRejected = allGuarantors.stream().anyMatch(g -> g.getStatus() == Guarantor.GuarantorStatus.DECLINED);
        boolean allAccepted = allGuarantors.stream().allMatch(g -> g.getStatus() == Guarantor.GuarantorStatus.ACCEPTED);

        if (anyRejected) {
            log.info("Loan {} has rejected guarantors.", loan.getLoanNumber());
        }
        else if (allAccepted) {
            // ✅ SUCCESS: Everyone signed! Move to SUBMITTED (Officer Queue)
            loan.setLoanStatus(Loan.LoanStatus.SUBMITTED);
            loanRepository.save(loan);

            // Notify Applicant
            try {
                emailService.sendEmail(
                        loan.getMember().getEmail(),
                        "Loan Application Forwarded",
                        "Great news! All your guarantors have accepted. Your application has been forwarded to the Loan Officer for final review."
                );
            } catch (Exception e) {
                log.error("Failed to send final submission email", e);
            }
        }
    }

    // ✅ NEW: Resend Notification Logic
    @Transactional
    public void resendGuarantorNotification(UUID guarantorId) {
        Guarantor guarantor = guarantorRepository.findById(guarantorId)
                .orElseThrow(() -> new ApiException("Guarantor request not found", 404));

        if (guarantor.getStatus() != Guarantor.GuarantorStatus.PENDING) {
            throw new ApiException("Cannot remind a guarantor who has already responded.", 400);
        }

        // Re-use the email logic
        try {
            String subject = "Reminder: Guarantorship Request";
            String message = String.format(
                    "Hello %s,\n\n" +
                            "This is a reminder that %s %s has requested you to guarantee their loan.\n" +
                            "Loan Amount: %s %s\n" +
                            "Requested Guarantee: %s %s\n\n" +
                            "Please log in to your dashboard to Approve or Reject this request.",
                    guarantor.getMember().getFirstName(),
                    guarantor.getLoan().getMember().getFirstName(), guarantor.getLoan().getMember().getLastName(),
                    guarantor.getLoan().getCurrencyCode(), guarantor.getLoan().getPrincipalAmount(),
                    guarantor.getLoan().getCurrencyCode(), guarantor.getGuaranteedAmount()
            );
            emailService.sendEmail(guarantor.getMember().getEmail(), subject, message);
        } catch (Exception e) {
            log.error("Failed to resend email", e);
            throw new ApiException("Failed to send email. Please check system logs.", 500);
        }
    }

    // --- STEP 6: SUBMIT APPLICATION (Submit & Wait Logic) ---
    @Transactional
    public void submitApplication(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

        if (loan.getLoanStatus() != Loan.LoanStatus.PENDING_GUARANTORS) {
            throw new ApiException("Loan cannot be submitted. Current status: " + loan.getLoanStatus(), 400);
        }

        long pendingGuarantors = guarantorRepository.findAllByLoan(loan).stream()
                .filter(g -> g.getStatus() == Guarantor.GuarantorStatus.PENDING)
                .count();

        if (pendingGuarantors > 0) {
            // Case A: Guarantors exist but haven't approved yet -> Set to AWAITING_GUARANTORS
            loan.setLoanStatus(Loan.LoanStatus.AWAITING_GUARANTORS);
            try {
                emailService.sendEmail(
                        loan.getMember().getEmail(),
                        "Application Waiting for Guarantors",
                        "Your application has been saved. We have notified your guarantors. Once they accept, it will be forwarded automatically."
                );
            } catch (Exception e) {
                log.error("Failed to send submission email", e);
            }
        } else {
            // Case B: No guarantors (Self-guaranteed) OR All already accepted -> Submit to Officer
            loan.setLoanStatus(Loan.LoanStatus.SUBMITTED);
            try {
                emailService.sendEmail(
                        loan.getMember().getEmail(),
                        "Application Submitted",
                        "Your application has been received and is under review."
                );
            } catch (Exception e) {
                log.error("Failed to send submission email", e);
            }
        }
        loanRepository.save(loan);
    }
    // --- STEP 7: DELETE UNFINISHED LOAN ---
    @Transactional
    public void deleteLoanApplication(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan application not found", 404));

        // strict check: Only allow deleting loans that are still being built
        boolean isDeletable = loan.getLoanStatus() == Loan.LoanStatus.DRAFT ||
                loan.getLoanStatus() == Loan.LoanStatus.PENDING_GUARANTORS;

        if (!isDeletable) {
            throw new ApiException("Cannot delete this loan. It has already been submitted or processed.", 400);
        }

        // Delete the loan (Cascading will handle guarantors if configured, otherwise JPA handles it)
        loanRepository.delete(loan);
        log.info("Deleted unfinished loan application: {}", loan.getLoanNumber());
    }
}