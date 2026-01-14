package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDashboardDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanResponseDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanApplicationDraft;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepaymentSchedule;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanApplicationDraftRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentScheduleRepository;
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
    private final LoanRepaymentScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public LoanDashboardDTO getMemberDashboard(String email) {
        log.info("🔍 DASHBOARD LOOKUP: Searching for Member with email: ['{}']", email);

        Optional<Member> memberOpt = memberRepository.findByEmail(email);

        if (memberOpt.isEmpty()) {
            return LoanDashboardDTO.builder()
                    .activeLoans(Collections.emptyList())
                    .pendingApplications(Collections.emptyList())
                    .loansInProgress(Collections.emptyList())
                    .currentDraft(null)
                    .activeLoansCount(0)
                    .totalOutstandingBalance(0.0)
                    .build();
        }

        Member member = memberOpt.get();
        List<Loan> allLoans = loanRepository.findByMemberId(member.getId());

        // --- FILTERING ---
        // ✅ FIX: Added check for 'active' flag. If active=false, it is ignored.
        List<Loan> activeLoans = allLoans.stream()
                .filter(l -> Boolean.TRUE.equals(l.getActive())) // Check 1: Must be active record
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.ACTIVE ||
                        l.getLoanStatus() == Loan.LoanStatus.IN_ARREARS ||
                        l.getLoanStatus() == Loan.LoanStatus.DISBURSED ||
                        l.getLoanStatus() == Loan.LoanStatus.DEFAULTED)
                .collect(Collectors.toList());

        List<Loan> pendingApplications = allLoans.stream()
                .filter(l -> Boolean.TRUE.equals(l.getActive())) // Check 1: Must be active record
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.SUBMITTED ||
                        l.getLoanStatus() == Loan.LoanStatus.APPROVED ||
                        l.getLoanStatus() == Loan.LoanStatus.AWAITING_GUARANTORS)
                .collect(Collectors.toList());

        List<Loan> loansInProgress = allLoans.stream()
                .filter(l -> Boolean.TRUE.equals(l.getActive())) // Check 1: Must be active record
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.PENDING_GUARANTORS)
                .collect(Collectors.toList());

        Optional<LoanApplicationDraft> currentDraft = draftRepository.findFirstByMemberIdAndStatusIn(
                member.getId(),
                Arrays.asList(LoanApplicationDraft.DraftStatus.PENDING_FEE, LoanApplicationDraft.DraftStatus.FEE_PAID)
        );

        // Calculate Totals for the Dashboard Header
        double totalBalance = activeLoans.stream()
                .map(Loan::getTotalOutstandingAmount)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        // Note: The mapToDTO call below handles fetching the next payment date & installment number
        return LoanDashboardDTO.builder()
                .activeLoans(activeLoans.stream().map(this::mapToDTO).collect(Collectors.toList()))
                .pendingApplications(pendingApplications.stream().map(this::mapToDTO).collect(Collectors.toList()))
                .loansInProgress(loansInProgress.stream().map(this::mapToDTO).collect(Collectors.toList()))
                .currentDraft(currentDraft.orElse(null))
                .activeLoansCount(activeLoans.size())
                .totalOutstandingBalance(totalBalance)
                .build();
    }

    public List<LoanResponseDTO> getMemberLoans(String email) {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);
        if (memberOpt.isEmpty()) return Collections.emptyList();

        return loanRepository.findByMemberId(memberOpt.get().getId()).stream()
                .filter(l -> Boolean.TRUE.equals(l.getActive())) // ✅ FIX: Only show active loans in list
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ... (Keep getGuarantorRequests, getLoanGuarantors, getPendingVotes as they were) ...
    public List<Map<String, Object>> getGuarantorRequests(String email) {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);
        if (memberOpt.isEmpty()) return Collections.emptyList();
        List<Guarantor> requests = guarantorRepository.findByMemberAndStatus(memberOpt.get(), Guarantor.GuarantorStatus.PENDING);
        return requests.stream().map(g -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", g.getId());
            data.put("applicantName", g.getLoan().getMember().getFirstName() + " " + g.getLoan().getMember().getLastName());
            data.put("applicantMemberNumber", g.getLoan().getMember().getMemberNumber());
            data.put("guaranteeAmount", g.getGuaranteedAmount() != null ? g.getGuaranteedAmount() : BigDecimal.ZERO);
            data.put("loanNumber", g.getLoan().getLoanNumber());
            data.put("loanProduct", g.getLoan().getProduct().getProductName());
            data.put("loanAmount", g.getLoan().getPrincipalAmount());
            data.put("applicationDate", g.getLoan().getApplicationDate() != null ? g.getLoan().getApplicationDate().toString() : LocalDate.now().toString());
            return data;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getLoanGuarantors(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new ApiException("Loan not found", 404));
        List<Guarantor> guarantors = guarantorRepository.findAllByLoan(loan);
        return guarantors.stream().map(g -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", g.getId());
            map.put("memberId", g.getMember().getId());
            map.put("firstName", g.getMember().getFirstName());
            map.put("lastName", g.getMember().getLastName());
            map.put("memberNumber", g.getMember().getMemberNumber());
            map.put("guaranteedAmount", g.getGuaranteedAmount());
            map.put("status", g.getStatus());
            return map;
        }).collect(Collectors.toList());
    }

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

    // ✅ KEY UPDATE: mapToDTO now populates nextPaymentDate and nextInstallmentNumber
    private LoanResponseDTO mapToDTO(Loan loan) {
        BigDecimal outstandingAmount = loan.getTotalOutstandingAmount();

        if ((loan.getLoanStatus() == Loan.LoanStatus.DISBURSED ||
                loan.getLoanStatus() == Loan.LoanStatus.ACTIVE) &&
                (outstandingAmount == null || outstandingAmount.compareTo(BigDecimal.ZERO) == 0)) {
            BigDecimal principal = loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal() : loan.getDisbursedAmount();
            BigDecimal interest = loan.getOutstandingInterest() != null ? loan.getOutstandingInterest() : BigDecimal.ZERO;
            outstandingAmount = principal != null ? principal.add(interest) : BigDecimal.ZERO;
        }

        if (outstandingAmount == null) outstandingAmount = BigDecimal.ZERO;

        // 1. Try fetching from Entity first (if manually populated)
        LocalDate nextDate = loan.getNextPaymentDate();
        Integer nextInstallment = null;

        // 2. If missing, auto-fetch from Schedule using repository
        if (nextDate == null && (loan.getLoanStatus() == Loan.LoanStatus.ACTIVE || loan.getLoanStatus() == Loan.LoanStatus.IN_ARREARS || loan.getLoanStatus() == Loan.LoanStatus.DISBURSED || loan.getLoanStatus() == Loan.LoanStatus.DEFAULTED)) {
            Optional<LoanRepaymentSchedule> nextSchedule = scheduleRepository.findTopByLoanIdAndStatusInOrderByDueDateAsc(
                    loan.getId(),
                    Arrays.asList(
                            LoanRepaymentSchedule.InstallmentStatus.PENDING,
                            LoanRepaymentSchedule.InstallmentStatus.PARTIALLY_PAID,
                            LoanRepaymentSchedule.InstallmentStatus.OVERDUE
                    )
            );
            if (nextSchedule.isPresent()) {
                nextDate = nextSchedule.get().getDueDate();
                nextInstallment = nextSchedule.get().getInstallmentNumber(); // ✅ Capture Installment #
            }
        }

        return LoanResponseDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .productName(loan.getProduct().getProductName())
                .principalAmount(loan.getPrincipalAmount())
                .totalOutstandingAmount(outstandingAmount)
                .loanStatus(loan.getLoanStatus().name())
                .applicationDate(loan.getApplicationDate())
                .feePaid(loan.isFeePaid())
                .weeklyRepaymentAmount(loan.getWeeklyRepaymentAmount() != null ? loan.getWeeklyRepaymentAmount() : BigDecimal.ZERO)
                .totalArrears(loan.getTotalArrears() != null ? loan.getTotalArrears() : BigDecimal.ZERO)
                .totalPrepaid(loan.getTotalPrepaid() != null ? loan.getTotalPrepaid() : BigDecimal.ZERO)
                .nextPaymentDate(nextDate)
                .nextInstallmentNumber(nextInstallment) // ✅ Pass to Frontend
                .build();
    }

    // ... (Keep existing loan officer methods) ...
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingLoansForOfficer() {
        List<Loan> pendingLoans = loanRepository.findByLoanStatusIn(Arrays.asList(Loan.LoanStatus.SUBMITTED, Loan.LoanStatus.UNDER_REVIEW));
        return pendingLoans.stream().map(this::mapLoanToOfficerMap).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllLoansForOfficer() {
        List<Loan> allLoans = loanRepository.findByLoanStatusIn(Arrays.asList(
                Loan.LoanStatus.SUBMITTED, Loan.LoanStatus.UNDER_REVIEW, Loan.LoanStatus.APPROVED,
                Loan.LoanStatus.REJECTED, Loan.LoanStatus.DISBURSED, Loan.LoanStatus.ACTIVE,
                Loan.LoanStatus.CLOSED, Loan.LoanStatus.CANCELLED
        ));
        return allLoans.stream().map(this::mapLoanToOfficerMap).collect(Collectors.toList());
    }

    private Map<String, Object> mapLoanToOfficerMap(Loan loan) {
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
        loanData.put("guarantorsApproved", loan.getGuarantors().stream().filter(g -> g.getStatus() == Guarantor.GuarantorStatus.ACCEPTED).count());
        return loanData;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLoanOfficerStatistics() {
        List<Loan> allLoans = loanRepository.findAll();
        long submitted = allLoans.stream().filter(l -> l.getLoanStatus() == Loan.LoanStatus.SUBMITTED).count();
        long underReview = allLoans.stream().filter(l -> l.getLoanStatus() == Loan.LoanStatus.UNDER_REVIEW).count();
        long approved = allLoans.stream().filter(l -> l.getLoanStatus() == Loan.LoanStatus.APPROVED).count();
        long rejected = allLoans.stream().filter(l -> l.getLoanStatus() == Loan.LoanStatus.REJECTED).count();
        long active = allLoans.stream().filter(l -> l.getLoanStatus() == Loan.LoanStatus.ACTIVE || l.getLoanStatus() == Loan.LoanStatus.DISBURSED || l.getLoanStatus() == Loan.LoanStatus.IN_ARREARS).count();

        BigDecimal totalDisbursed = allLoans.stream()
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.DISBURSED || l.getLoanStatus() == Loan.LoanStatus.ACTIVE || l.getLoanStatus() == Loan.LoanStatus.IN_ARREARS || l.getLoanStatus() == Loan.LoanStatus.DEFAULTED || l.getLoanStatus() == Loan.LoanStatus.CLOSED)
                .filter(l -> l.getDisbursedAmount() != null && l.getDisbursedAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Loan::getDisbursedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = allLoans.stream()
                .filter(l -> l.getLoanStatus() == Loan.LoanStatus.ACTIVE || l.getLoanStatus() == Loan.LoanStatus.IN_ARREARS || l.getLoanStatus() == Loan.LoanStatus.DEFAULTED)
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