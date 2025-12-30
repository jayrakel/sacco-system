package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.loan.api.dto.LoanRequestDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationService {

    private final LoanRepository loanRepository;
    private final LoanProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final GuarantorRepository guarantorRepository;
    private final SavingsAccountRepository savingsRepository;
    private final LoanEligibilityService eligibilityService;

    @Transactional
    // ✅ CHANGED: Accept Email
    public Loan createDraft(String email, LoanRequestDTO request) {
        // 1. Enforce Eligibility
        Map<String, Object> eligibility = eligibilityService.checkEligibility(email);
        if (!(boolean) eligibility.get("eligible")) {
            throw new ApiException("Application Rejected: " + eligibility.get("reasons"), 400);
        }

        // 2. Resolve Member
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member profile not found", 400));

        LoanProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ApiException("Product not found", 404));

        String loanNumber = "LN-" + (int)(Math.random() * 1000000);

        Loan loan = Loan.builder()
                .member(member)
                .product(product)
                .loanNumber(loanNumber)
                .principalAmount(request.getAmount())
                .interestRate(product.getInterestRate())
                .durationWeeks(request.getDurationWeeks())
                .status(Loan.LoanStatus.DRAFT)
                .applicationDate(LocalDate.now())
                .build();

        return loanRepository.save(loan);
    }

    // ... (Add Guarantor and Submit methods remain using Loan ID, which is fine) ...
    @Transactional
    public void addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        // ... (Keep existing implementation)
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        // ...
    }

    @Transactional
    public void submitApplication(UUID loanId) {
        // ... (Keep existing implementation)
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setStatus(Loan.LoanStatus.SUBMITTED);
        loanRepository.save(loan);
    }
}