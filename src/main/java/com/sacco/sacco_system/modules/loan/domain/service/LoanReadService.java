package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDashboardDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanResponseDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanApplicationDraft;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanApplicationDraftRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanReadService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final GuarantorRepository guarantorRepository;
    private final LoanEligibilityService eligibilityService;
    private final LoanApplicationDraftRepository draftRepository;

    @Transactional(readOnly = true)
    public LoanDashboardDTO getMemberDashboard(String email) {
        log.info("🔍 DASHBOARD LOOKUP: Searching for Member with email: ['{}']", email);

        Optional<Member> memberOpt = memberRepository.findByEmail(email);

        // Handle non-member Users gracefully
        if (memberOpt.isEmpty()) {
            return LoanDashboardDTO.builder()
                    .activeLoans(Collections.emptyList())
                    .pendingApplications(Collections.emptyList())
                    .loansInProgress(Collections.emptyList())
                    .currentDraft(null)
                    .build();
        }

        Member member = memberOpt.get();

        // 1. Fetch all loans for the member
        List<Loan> allLoans = loanRepository.findByMemberId(member.getId());

        // 2. Filter: Active Loans (Running or In Arrears)
        List<Loan> activeLoans = allLoans.stream()
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.ACTIVE || l.getLoanStatus() == Loan.LoanStatus.IN_ARREARS)
                .collect(Collectors.toList());

        // 3. Filter: Pending Applications (Submitted for approval)
        List<Loan> pendingApplications = allLoans.stream()
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.SUBMITTED || l.getLoanStatus() == Loan.LoanStatus.APPROVED)
                .collect(Collectors.toList());

        // 4. ✅ Loans In Progress (Stuck at Guarantors Step)
        List<Loan> loansInProgress = allLoans.stream()
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.PENDING_GUARANTORS)
                .collect(Collectors.toList());

        // 5. ✅ Fetch Current Draft (Stuck at Fee Payment or Details)
        Optional<LoanApplicationDraft> currentDraft = draftRepository.findFirstByMemberIdAndStatusIn(
                member.getId(),
                Arrays.asList(LoanApplicationDraft.DraftStatus.PENDING_FEE, LoanApplicationDraft.DraftStatus.FEE_PAID)
        );

        return LoanDashboardDTO.builder()
                .activeLoans(activeLoans)
                .pendingApplications(pendingApplications)
                .loansInProgress(loansInProgress)
                .currentDraft(currentDraft.orElse(null))
                .build();
    }

    public List<LoanResponseDTO> getMemberLoans(String email) {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);

        if (memberOpt.isEmpty()) {
            return Collections.emptyList();
        }

        return loanRepository.findByMemberId(memberOpt.get().getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Returns requests where other members have asked the current user to be a guarantor.
     */
    public List<Map<String, Object>> getGuarantorRequests(String email) {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);

        if (memberOpt.isEmpty()) {
            return Collections.emptyList();
        }

        List<Guarantor> requests = guarantorRepository.findByMemberAndStatus(memberOpt.get(), Guarantor.GuarantorStatus.PENDING);

        return requests.stream().map(g -> Map.<String, Object>of(
                "requestId", g.getId(),
                "borrowerName", g.getLoan().getMember().getFirstName() + " " + g.getLoan().getMember().getLastName(),
                "amount", g.getGuaranteedAmount() != null ? g.getGuaranteedAmount() : BigDecimal.ZERO,
                "loanType", g.getLoan().getProduct().getProductName(),
                "dateRequested", g.getLoan().getApplicationDate() != null ? g.getLoan().getApplicationDate() : LocalDate.now()
        )).collect(Collectors.toList());
    }

    /**
     * ✅ NEW: Fetch all guarantors for a specific loan
     * Used by the UI to restore state when "Resuming" an application.
     */
    public List<Map<String, Object>> getLoanGuarantors(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        List<Guarantor> guarantors = guarantorRepository.findAllByLoan(loan);

        return guarantors.stream().map(g -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", g.getId());
            map.put("memberId", g.getMember().getId());
            map.put("firstName", g.getMember().getFirstName());
            map.put("lastName", g.getMember().getLastName());
            map.put("memberNumber", g.getMember().getMemberNumber());
            map.put("guaranteedAmount", g.getGuaranteedAmount());
            map.put("status", g.getStatus()); // PENDING, ACCEPTED, REJECTED
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Returns loans awaiting approval (SUBMITTED status).
     */
    public List<Map<String, Object>> getPendingVotes(String email) {
        List<Loan> loans = loanRepository.findByLoanStatus(Loan.LoanStatus.SUBMITTED);

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
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private LoanResponseDTO mapToDTO(Loan loan) {
        return LoanResponseDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .productName(loan.getProduct().getProductName())
                .principalAmount(loan.getPrincipalAmount())
                .totalOutstandingAmount(loan.getTotalOutstandingAmount())
                .loanStatus(loan.getLoanStatus().name())
                .applicationDate(loan.getApplicationDate())
                .feePaid(loan.isFeePaid())
                .build();
    }
}