package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.finance.domain.service.TransactionService;
import com.sacco.sacco_system.modules.loan.api.dto.LoanRequestDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
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
    private final LoanProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final LoanEligibilityService eligibilityService;
    private final TransactionService transactionService;

    // --- STEP 1: INITIATE ---
    @Transactional
    public Loan initiateApplication(String email, LoanRequestDTO request) {
        log.info("Initiating loan application for {}", email);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member profile not found", 404));

        if (!member.getMemberStatus().equals(Member.MemberStatus.ACTIVE)) {
            throw new ApiException("Only active members can apply for loans", 400);
        }

        // Resumption Check
        List<Loan.LoanStatus> incompleteStatuses = Arrays.asList(
                Loan.LoanStatus.DRAFT,
                Loan.LoanStatus.PENDING_GUARANTORS
        );
        Optional<Loan> existingLoan = loanRepository.findByMemberIdAndLoanStatusIn(member.getId(), incompleteStatuses)
                .stream().findFirst();

        if (existingLoan.isPresent()) {
            return existingLoan.get();
        }

        // Validate Product
        LoanProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ApiException("Product not found", 404));

        // Eligibility Check
        Map<String, Object> eligibility = eligibilityService.checkEligibility(email);
        if (!(boolean) eligibility.get("eligible")) {
            throw new ApiException("Application Rejected: " + eligibility.get("reasons"), 400);
        }

        // Validate Amount Limits
        if (request.getAmount().compareTo(product.getMinAmount()) < 0) {
            throw new ApiException("Amount is below minimum limit of " + product.getMinAmount(), 400);
        }
        if (request.getAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new ApiException("Amount exceeds product limit of " + product.getMaxAmount(), 400);
        }

        // Create Draft
        String loanNumber = "LN-" + (100000 + (long)(Math.random() * 900000));

        Loan loan = Loan.builder()
                .member(member)
                .product(product)
                .loanNumber(loanNumber)
                .principalAmount(request.getAmount())
                .interestRate(product.getInterestRate())
                .durationWeeks(request.getDurationWeeks())
                .loanStatus(Loan.LoanStatus.DRAFT)
                .applicationDate(LocalDate.now())
                .feePaid(false)
                .totalOutstandingAmount(BigDecimal.ZERO)
                .build();

        return loanRepository.save(loan);
    }

    // --- STEP 2: CONFIRM FEE ---
    @Transactional
    public Loan confirmApplicationFee(UUID loanId, String paymentReference) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan application not found", 404));

        if (loan.isFeePaid()) return loan;

        LoanProduct product = loan.getProduct();
        BigDecimal fee = product.getApplicationFee();

        if (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) {
            transactionService.recordProcessingFee(
                    loan.getMember(),
                    fee,
                    paymentReference,
                    product.getIncomeAccountCode()
            );
        }

        loan.setFeePaid(true);
        loan.setLoanStatus(Loan.LoanStatus.PENDING_GUARANTORS);

        return loanRepository.save(loan);
    }

    // --- STEP 3: ADD GUARANTORS (Restored) ---
    @Transactional
    public void addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        // NOTE: We will build the full logic here in the next step.
        // For now, this placeholder allows the Controller to compile.
        log.info("Adding guarantor {} to loan {} with amount {}", guarantorMemberId, loanId, amount);
    }

    // --- STEP 4: SUBMIT (Restored) ---
    @Transactional
    public void submitApplication(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

        loan.setLoanStatus(Loan.LoanStatus.SUBMITTED);
        loanRepository.save(loan);
        log.info("Loan {} submitted for review", loan.getLoanNumber());
    }
}