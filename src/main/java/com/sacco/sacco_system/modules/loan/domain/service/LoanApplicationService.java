package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository; // ✅ Added Import
import com.sacco.sacco_system.modules.finance.domain.service.TransactionService;
import com.sacco.sacco_system.modules.loan.api.dto.LoanRequestDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanApplicationDraft;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanApplicationDraftRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final TransactionRepository transactionRepository; // ✅ Injected to check duplicates

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

    // --- STEP 2: CONFIRM FEE (Fixed Duplicate Transaction Bug) ---
    @Transactional
    public LoanApplicationDraft confirmDraftFee(UUID draftId, String paymentReference) {
        LoanApplicationDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new ApiException("Draft application not found", 404));

        if (draft.isFeePaid()) return draft;

        // ✅ CRITICAL FIX: Check if PaymentService already created the transaction
        boolean transactionExists = transactionRepository.findByExternalReference(paymentReference).isPresent();

        if (!transactionExists) {
            // Only record if PaymentService didn't catch it
            transactionService.recordProcessingFee(
                    draft.getMember(),
                    new BigDecimal("500"),
                    paymentReference,
                    "4000"
            );
        } else {
            log.info("Transaction {} already exists. Skipping ledger recording.", paymentReference);
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

        if (request.getAmount().compareTo(product.getMinAmount()) < 0 ||
                request.getAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new ApiException("Amount is outside product limits", 400);
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