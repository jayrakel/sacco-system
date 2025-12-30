package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDashboardDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanResponseDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanReadService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final LoanEligibilityService eligibilityService;

    @Transactional(readOnly = true)
    // ✅ CHANGED: Accept Email
    public LoanDashboardDTO getMemberDashboard(String email) {
        // ✅ Safe Lookup by Email
        log.info("🔍 DASHBOARD LOOKUP: Searching for Member with email: ['{}']", email);
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member profile not found. Please complete your registration.", 400));

        // 1. Check Eligibility (Pass Email)
        Map<String, Object> eligibility = eligibilityService.checkEligibility(email);
        boolean isEligible = (boolean) eligibility.get("eligible");

        // 2. Fetch Loans (Using Member ID resolved above)
        List<Loan> loans = loanRepository.findByMemberId(member.getId());

        // 3. Convert to DTOs
        List<LoanResponseDTO> loanDTOs = loans.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        BigDecimal totalBalance = loans.stream()
                .map(Loan::getLoanBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return LoanDashboardDTO.builder()
                .canApply(isEligible)
                .eligibilityMessage(isEligible
                        ? "You are eligible to apply for a new loan."
                        : "You are currently not eligible for a new loan.")
                .eligibilityDetails(eligibility)
                .activeLoans(loanDTOs)
                .activeLoansCount(loans.size())
                .totalOutstandingBalance(totalBalance.doubleValue())
                .build();
    }

    // ✅ CHANGED: Accept Email
    public List<LoanResponseDTO> getMemberLoans(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member not found", 400));
        return loanRepository.findByMemberId(member.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private LoanResponseDTO mapToDTO(Loan loan) {
        return LoanResponseDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .productName(loan.getProduct().getName())
                .principalAmount(loan.getPrincipalAmount())
                .balance(loan.getLoanBalance())
                .status(loan.getStatus().name())
                .applicationDate(loan.getApplicationDate())
                .build();
    }
}