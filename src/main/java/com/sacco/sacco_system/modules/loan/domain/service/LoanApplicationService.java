package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.api.dto.LoanRequestDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanResponseDTO;
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
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationService {
    private final LoanRepository loanRepository;
    private final LoanProductRepository loanProductRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public LoanResponseDTO createDraft(UUID userId, LoanRequestDTO request) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        LoanProduct product = loanProductRepository.findById(request.getProductId()).orElseThrow();

        if (request.getAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new RuntimeException("Amount exceeds limit");
        }

        Loan loan = Loan.builder()
                .loanNumber("LN-" + System.currentTimeMillis())
                .member(member)
                .product(product)
                .principalAmount(request.getAmount())
                .durationWeeks(request.getDurationWeeks())
                .interestRate(product.getInterestRate())
                .status(Loan.LoanStatus.DRAFT)
                .applicationDate(LocalDate.now())
                .build();

        Loan saved = loanRepository.save(loan);
        log.info("Loan Draft Created: {}", saved.getLoanNumber());

        return mapToDTO(saved);
    }

    private LoanResponseDTO mapToDTO(Loan loan) {
        return LoanResponseDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .productName(loan.getProduct().getName())
                .principalAmount(loan.getPrincipalAmount())
                .status(loan.getStatus().name())
                .applicationDate(loan.getApplicationDate())
                .build();
    }
}