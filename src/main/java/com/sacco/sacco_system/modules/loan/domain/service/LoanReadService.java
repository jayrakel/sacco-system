package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDashboardDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanResponseDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
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
    private final GuarantorRepository guarantorRepository;
    private final LoanEligibilityService eligibilityService;

    @Transactional(readOnly = true)
    public LoanDashboardDTO getMemberDashboard(String email) {
        log.info("🔍 DASHBOARD LOOKUP: Searching for Member with email: ['{}']", email);
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member profile not found. Please complete your registration.", 400));

        // 1. Check Eligibility
        Map<String, Object> eligibility = eligibilityService.checkEligibility(email);
        boolean isEligible = (boolean) eligibility.get("eligible");

        // 2. Fetch Loans
        List<Loan> loans = loanRepository.findByMemberId(member.getId());

        // 3. Convert to DTOs
        List<LoanResponseDTO> loanDTOs = loans.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        BigDecimal totalBalance = loans.stream()
                .map(Loan::getTotalOutstandingAmount)
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

    public List<LoanResponseDTO> getMemberLoans(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member not found", 400));
        return loanRepository.findByMemberId(member.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Returns requests where other members have asked the current user to be a guarantor.
     */
    public List<Map<String, Object>> getGuarantorRequests(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member not found", 400));

        List<Guarantor> requests = guarantorRepository.findByMemberAndStatus(member, Guarantor.GuarantorStatus.PENDING);

        // ✅ FIXED: Explicitly typed Map.<String, Object>of() to prevent type inference errors
        return requests.stream().map(g -> Map.<String, Object>of(
                "requestId", g.getId(),
                "borrowerName", g.getLoan().getMember().getFirstName() + " " + g.getLoan().getMember().getLastName(),
                "amount", g.getGuaranteedAmount() != null ? g.getGuaranteedAmount() : BigDecimal.ZERO,
                "loanType", g.getLoan().getProduct().getProductName(),
                "dateRequested", g.getLoan().getApplicationDate()
        )).collect(Collectors.toList());
    }

    /**
     * Returns loans awaiting approval (SUBMITTED status).
     */
    public List<Map<String, Object>> getPendingVotes(String email) {
        // In real apps, verify user role here (e.g., Committee Member)
        List<Loan> loans = loanRepository.findByLoanStatus(Loan.LoanStatus.SUBMITTED);

        // ✅ FIXED: Explicitly typed Map.<String, Object>of()
        return loans.stream().map(l -> Map.<String, Object>of(
                "id", l.getId(),
                "applicantName", l.getMember().getFirstName() + " " + l.getMember().getLastName(),
                "amount", l.getPrincipalAmount(),
                "productName", l.getProduct().getProductName(),
                "dateSubmitted", l.getApplicationDate()
        )).collect(Collectors.toList());
    }

    public List<LoanResponseDTO> getAllLoans() {
        return loanRepository.findAll().stream()
                .map(this::mapToDTO) // Reuses your existing mapToDTO
                .collect(Collectors.toList());
    }

    private LoanResponseDTO mapToDTO(Loan loan) {
        return LoanResponseDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .productName(loan.getProduct().getProductName())
                .principalAmount(loan.getPrincipalAmount())
                .balance(loan.getTotalOutstandingAmount())
                .status(loan.getLoanStatus().name())
                .applicationDate(loan.getApplicationDate())
                .feePaid(loan.isFeePaid())
                .build();
    }
}