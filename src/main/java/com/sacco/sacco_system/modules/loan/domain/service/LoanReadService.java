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
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.ACTIVE ||
                        l.getLoanStatus() == Loan.LoanStatus.IN_ARREARS ||
                        l.getLoanStatus() == Loan.LoanStatus.DISBURSED)
                .collect(Collectors.toList());

        // 3. Filter: Pending Applications (Submitted, Approved, or Waiting for Guarantors)
        // ✅ UPDATE: Added AWAITING_GUARANTORS so users see these as "Submitted/In Progress"
        List<Loan> pendingApplications = allLoans.stream()
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.SUBMITTED ||
                        l.getLoanStatus() == Loan.LoanStatus.APPROVED ||
                        l.getLoanStatus() == Loan.LoanStatus.AWAITING_GUARANTORS)
                .collect(Collectors.toList());

        // 4. Loans In Progress (Stuck at Guarantors Step - "Resume" mode)
        List<Loan> loansInProgress = allLoans.stream()
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.PENDING_GUARANTORS)
                .collect(Collectors.toList());

        // 5. Fetch Current Draft (Stuck at Fee Payment or Details)
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

        return requests.stream().map(g -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", g.getId());  // ✅ Match frontend expectation
            data.put("applicantName", g.getLoan().getMember().getFirstName() + " " + g.getLoan().getMember().getLastName());  // ✅ Match frontend
            data.put("applicantMemberNumber", g.getLoan().getMember().getMemberNumber());  // ✅ New field
            data.put("guaranteeAmount", g.getGuaranteedAmount() != null ? g.getGuaranteedAmount() : BigDecimal.ZERO);  // ✅ Match frontend
            data.put("loanNumber", g.getLoan().getLoanNumber());  // ✅ New field
            data.put("loanProduct", g.getLoan().getProduct().getProductName());  // ✅ New field
            data.put("loanAmount", g.getLoan().getPrincipalAmount());  // ✅ Total loan amount
            data.put("applicationDate", g.getLoan().getApplicationDate() != null ? g.getLoan().getApplicationDate().toString() : LocalDate.now().toString());
            return data;
        }).collect(Collectors.toList());
    }

    /**
     * Fetch all guarantors for a specific loan
     * Used by the UI to restore state when "Resuming" an application.
     */
    public List<Map<String, Object>> getLoanGuarantors(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

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

    /**
     * Get all loans pending review for loan officers
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingLoansForOfficer() {
        List<Loan> pendingLoans = loanRepository.findByLoanStatusIn(Arrays.asList(
                Loan.LoanStatus.SUBMITTED,
                Loan.LoanStatus.UNDER_REVIEW
        ));

        return pendingLoans.stream().map(loan -> {
            Map<String, Object> loanData = new HashMap<>();
            loanData.put("id", loan.getId());
            loanData.put("loanNumber", loan.getLoanNumber());
            loanData.put("memberName", loan.getMember().getFirstName() + " " + loan.getMember().getLastName());
            loanData.put("memberNumber", loan.getMember().getMemberNumber());
            loanData.put("productName", loan.getProduct().getProductName());
            loanData.put("principalAmount", loan.getPrincipalAmount());
            loanData.put("durationWeeks", loan.getDurationWeeks());
            loanData.put("interestRate", loan.getInterestRate());
            loanData.put("status", loan.getLoanStatus().name());
            loanData.put("applicationDate", loan.getApplicationDate());
            loanData.put("guarantorsCount", loan.getGuarantors().size());
            loanData.put("guarantorsApproved", loan.getGuarantors().stream()
                    .filter(g -> g.getStatus() == Guarantor.GuarantorStatus.ACCEPTED)
                    .count());
            return loanData;
        }).collect(Collectors.toList());
    }

    /**
     * Get ALL loans for loan officer dashboard (supports all tabs: pending, approved, rejected, all)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllLoansForOfficer() {
        // Get all loans that have been submitted (exclude DRAFT and PENDING_GUARANTORS)
        List<Loan> allLoans = loanRepository.findByLoanStatusIn(Arrays.asList(
                Loan.LoanStatus.SUBMITTED,
                Loan.LoanStatus.UNDER_REVIEW,
                Loan.LoanStatus.APPROVED,
                Loan.LoanStatus.REJECTED,
                Loan.LoanStatus.DISBURSED,
                Loan.LoanStatus.ACTIVE,
                Loan.LoanStatus.CLOSED,
                Loan.LoanStatus.CANCELLED
        ));

        return allLoans.stream().map(loan -> {
            Map<String, Object> loanData = new HashMap<>();
            loanData.put("id", loan.getId());
            loanData.put("loanNumber", loan.getLoanNumber());
            loanData.put("memberName", loan.getMember().getFirstName() + " " + loan.getMember().getLastName());
            loanData.put("memberNumber", loan.getMember().getMemberNumber());
            loanData.put("productName", loan.getProduct().getProductName());
            loanData.put("principalAmount", loan.getPrincipalAmount());
            loanData.put("durationWeeks", loan.getDurationWeeks());
            loanData.put("interestRate", loan.getInterestRate());
            loanData.put("status", loan.getLoanStatus().name());
            loanData.put("applicationDate", loan.getApplicationDate());
            loanData.put("guarantorsCount", loan.getGuarantors().size());
            loanData.put("guarantorsApproved", loan.getGuarantors().stream()
                    .filter(g -> g.getStatus() == Guarantor.GuarantorStatus.ACCEPTED)
                    .count());
            return loanData;
        }).collect(Collectors.toList());
    }

    /**
     * Get statistics for loan officer dashboard
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLoanOfficerStatistics() {
        List<Loan> allLoans = loanRepository.findAll();

        long submitted = allLoans.stream().filter(l -> l.getLoanStatus() == Loan.LoanStatus.SUBMITTED).count();
        long underReview = allLoans.stream().filter(l -> l.getLoanStatus() == Loan.LoanStatus.UNDER_REVIEW).count();
        long approved = allLoans.stream().filter(l -> l.getLoanStatus() == Loan.LoanStatus.APPROVED).count();
        long rejected = allLoans.stream().filter(l -> l.getLoanStatus() == Loan.LoanStatus.REJECTED).count();
        long active = allLoans.stream().filter(l ->
                l.getLoanStatus() == Loan.LoanStatus.ACTIVE ||
                l.getLoanStatus() == Loan.LoanStatus.DISBURSED ||
                l.getLoanStatus() == Loan.LoanStatus.IN_ARREARS).count();

        // ✅ FIX: Only count disbursed amount from loans that are ACTUALLY disbursed
        BigDecimal totalDisbursed = allLoans.stream()
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.DISBURSED ||
                            l.getLoanStatus() == Loan.LoanStatus.ACTIVE ||
                            l.getLoanStatus() == Loan.LoanStatus.IN_ARREARS ||
                            l.getLoanStatus() == Loan.LoanStatus.DEFAULTED ||
                            l.getLoanStatus() == Loan.LoanStatus.CLOSED)
                .filter(l -> l.getDisbursedAmount() != null && l.getDisbursedAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Loan::getDisbursedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ✅ FIX: Only count outstanding from loans that are currently ACTIVE
        BigDecimal totalOutstanding = allLoans.stream()
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.ACTIVE ||
                            l.getLoanStatus() == Loan.LoanStatus.IN_ARREARS ||
                            l.getLoanStatus() == Loan.LoanStatus.DEFAULTED)
                .filter(l -> l.getTotalOutstandingAmount() != null && l.getTotalOutstandingAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Loan::getTotalOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingReview", submitted + underReview);
        stats.put("submitted", submitted);
        stats.put("underReview", underReview);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        stats.put("activeLoans", active);
        stats.put("totalDisbursed", totalDisbursed);
        stats.put("totalOutstanding", totalOutstanding);

        return stats;
    }
}