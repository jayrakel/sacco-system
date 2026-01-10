package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.TransactionService;
import com.sacco.sacco_system.modules.loan.api.dto.LoanRequestDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanApplicationDraft;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanApplicationDraftRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.EmploymentDetails; // ✅ Import
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode; // ✅ Import
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
    private final MemberRepository memberRepository;
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

        // Eligibility Check
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

    // --- STEP 3: CONVERT TO LOAN (WITH ALL GUARDRAILS) ---
    @Transactional
    public Loan createLoanFromDraft(UUID draftId, LoanRequestDTO request) {
        LoanApplicationDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new ApiException("Draft not found", 404));

        if (!draft.isFeePaid()) {
            throw new ApiException("Application fee not paid.", 400);
        }

        LoanProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ApiException("Product not found", 404));

        // 1. Validate Product Min/Max Amount
        if (request.getAmount().compareTo(product.getMinAmount()) < 0) {
            throw new ApiException("Amount is below the minimum allowed for this product (" + product.getMinAmount() + ")", 400);
        }
        if (request.getAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new ApiException("Amount exceeds the maximum allowed for this product (" + product.getMaxAmount() + ")", 400);
        }

        // 2. Validate Duration
        if (request.getDurationWeeks() > product.getMaxDurationWeeks()) {
            throw new ApiException("Duration exceeds the maximum allowed (" + product.getMaxDurationWeeks() + " weeks)", 400);
        }

        // 3. Validate Global Limit (Savings * Multiplier)
        BigDecimal memberLimit = eligibilityService.calculateMaxLoanLimit(draft.getMember());
        if (request.getAmount().compareTo(memberLimit) > 0) {
            throw new ApiException("Amount exceeds your eligible limit based on savings (KES " + memberLimit + ")", 400);
        }

        // 4. ✅ NEW GUARDRAIL: The 1/3rd Rule (Ability to Pay)
        validateAbilityToPay(draft.getMember(), request.getAmount(), request.getDurationWeeks(), product);

        String loanNumber = "LN-" + (100000 + (long)(Math.random() * 900000));

        // 5. ✅ NEW GUARDRAIL: Sacco Liquidity Check (Source of Truth)
        // Checks strictly against GL Balances minus Statutory Reserve
        BigDecimal lendableLiquidity = accountingService.calculateLendableLiquidity();

        if (request.getAmount().compareTo(lendableLiquidity) > 0) {
            log.warn("Loan Rejected: Liquidity Constraint. Req: {}, Lendable: {}", request.getAmount(), lendableLiquidity);
            throw new ApiException(
                    "The Sacco currently has insufficient lendable funds (Reserve Protection Active). Please try a smaller amount.",
                    400
            );
        }

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
     * ✅ Calculates estimated monthly installment and checks against 2/3 of Net Income.
     */
    private void validateAbilityToPay(Member member, BigDecimal amount, int durationWeeks, LoanProduct product) {
        EmploymentDetails employment = member.getEmploymentDetails();
        BigDecimal netIncome = (employment != null) ? employment.getNetMonthlyIncome() : BigDecimal.ZERO;

        // If income is not set, they fail this check (unless you want a bypass for Self-Guaranteed loans)
        if (netIncome == null || netIncome.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Valid employment income is required to assess ability to repay. Please update your profile.", 400);
        }

        // Convert Weeks to Months (Approximate: Weeks / 4)
        BigDecimal months = BigDecimal.valueOf(durationWeeks).divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
        if (months.compareTo(BigDecimal.ZERO) == 0) months = BigDecimal.ONE;

        // Determine Monthly Installment
        BigDecimal installment;
        BigDecimal rate = product.getInterestRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        if (product.getInterestType() == LoanProduct.InterestType.FLAT) {
            // Flat Rate: (P + (P * R * T)) / T
            BigDecimal totalInterest = amount.multiply(rate).multiply(months);
            BigDecimal totalPayable = amount.add(totalInterest);
            installment = totalPayable.divide(months, 2, RoundingMode.HALF_UP);
        } else {
            // Reducing Balance (Amortization)
            // PMT = P * ( r(1+r)^n ) / ( (1+r)^n - 1 )
            double r = rate.doubleValue(); // Monthly rate
            double n = months.doubleValue();

            if (r == 0) {
                installment = amount.divide(months, 2, RoundingMode.HALF_UP);
            } else {
                double numerator = r * Math.pow(1 + r, n);
                double denominator = Math.pow(1 + r, n) - 1;
                BigDecimal factor = BigDecimal.valueOf(numerator / denominator);
                installment = amount.multiply(factor);
            }
        }

        // The 1/3 Rule: Installment must not exceed 2/3 (66.67%) of Net Income
        BigDecimal maxAllowableInstallment = netIncome.multiply(new BigDecimal("0.6667"));

        if (installment.compareTo(maxAllowableInstallment) > 0) {
            throw new ApiException(String.format(
                    "Ability to Pay Check Failed: Estimated monthly installment (KES %s) exceeds 2/3 of your net income (KES %s). Please increase the duration or reduce the amount.",
                    installment.setScale(2, RoundingMode.HALF_UP),
                    maxAllowableInstallment.setScale(2, RoundingMode.HALF_UP)
            ), 400);
        }
    }

    // --- EXTRAS ---
    @Transactional
    public void addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {}

    @Transactional
    public void submitApplication(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setLoanStatus(Loan.LoanStatus.SUBMITTED);
        loanRepository.save(loan);
    }
}