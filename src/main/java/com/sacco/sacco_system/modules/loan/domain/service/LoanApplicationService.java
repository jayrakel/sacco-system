package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.finance.domain.service.TransactionService;
import com.sacco.sacco_system.modules.loan.api.dto.LoanRequestDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
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
    private final LoanEligibilityService eligibilityService;
    private final TransactionService transactionService;

    @Transactional(rollbackFor = Exception.class)
    public Loan createDraft(String email, LoanRequestDTO request) {
        log.info("Creating loan draft for {}", email);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member profile not found", 400));

        if (!member.getStatus().equals(Member.MemberStatus.ACTIVE)) {
            throw new ApiException("Only active members can apply for loans", 400);
        }

        LoanProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ApiException("Product not found", 404));

        Map<String, Object> eligibility = eligibilityService.checkEligibility(email);
        if (!(boolean) eligibility.get("eligible")) {
            throw new ApiException("Application Rejected: " + eligibility.get("reasons"), 400);
        }

        if (request.getAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new ApiException("Amount exceeds product limit of " + product.getMaxAmount(), 400);
        }

        if (product.getApplicationFee() != null && product.getApplicationFee().compareTo(BigDecimal.ZERO) > 0) {
            if (request.getPaymentReference() == null || request.getPaymentReference().isEmpty()) {
                throw new ApiException("Payment Required: Please pay the application fee.", 400);
            }

            transactionService.recordProcessingFee(
                    member,
                    product.getApplicationFee(),
                    request.getPaymentReference(),
                    product.getIncomeAccountCode()
            );
        }

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
                .feePaid(true)
                .build();

        return loanRepository.save(loan);
    }

    @Transactional
    public void addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

        Member guarantorMember = memberRepository.findById(guarantorMemberId)
                .orElseThrow(() -> new ApiException("Guarantor not found", 404));

        if (loan.getMember().getId().equals(guarantorMember.getId())) {
            throw new ApiException("You cannot guarantee your own loan", 400);
        }

        Guarantor guarantor = Guarantor.builder()
                .loan(loan)
                .member(guarantorMember)
                .guaranteeAmount(amount) // ✅ FIXED: Matches entity field name
                .dateAdded(LocalDate.now())
                .status(Guarantor.GuarantorStatus.PENDING)
                .build();

        guarantorRepository.save(guarantor);
        log.info("Added guarantor {} to loan {}", guarantorMember.getEmail(), loan.getLoanNumber());
    }

    @Transactional
    public void submitApplication(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

        loan.setStatus(Loan.LoanStatus.SUBMITTED);
        loanRepository.save(loan);
        log.info("Loan {} submitted for review", loan.getLoanNumber());
    }
}