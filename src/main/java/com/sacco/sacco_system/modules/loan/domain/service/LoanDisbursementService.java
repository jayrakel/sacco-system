package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanDisbursement;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanDisbursementRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.SystemSettingService;
import com.sacco.sacco_system.modules.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanDisbursementService {

    private final LoanDisbursementRepository disbursementRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final AccountingService accountingService;
    private final LoanRepaymentService repaymentService;
    private final SystemSettingService systemSettingService;
    private final NotificationService notificationService;

    /**
     * ✅ ONE-CLICK DISBURSEMENT (Finance Dashboard)
     */
    public void processDisbursement(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.TREASURER_DISBURSEMENT) {
            throw new RuntimeException("Loan is not ready for disbursement. Current status: " + loan.getStatus());
        }

        // Create Disbursement Record
        LoanDisbursement disbursement = LoanDisbursement.builder()
                .loan(loan)
                .member(loan.getMember())
                .amount(loan.getPrincipalAmount())
                .method(LoanDisbursement.DisbursementMethod.MPESA)
                .status(LoanDisbursement.DisbursementStatus.DISBURSED)
                .preparedBy(getCurrentUserId())
                .approvedBy(getCurrentUserId())
                .disbursedBy(getCurrentUserId())
                .approvedAt(LocalDateTime.now())
                .disbursedAt(LocalDateTime.now())
                .notes("Direct disbursement via Admin Dashboard")
                .build();

        disbursementRepository.save(disbursement);

        // ✅ FIX: Use shared helper to set status to ACTIVE
        updateLoanStatusAndFinancials(loan);

        // Accounting & Notify
        accountingService.postLoanDisbursement(loan, "1002");
        notificationService.notifyUser(loan.getMember().getId(), "Funds Disbursed",
                "KES " + loan.getPrincipalAmount() + " has been disbursed. Loan is now ACTIVE.", true, true);

        log.info("Loan {} processed and activated via Direct Process.", loan.getLoanNumber());
    }

    /**
     * TREASURER: Prepare disbursement
     */
    public LoanDisbursement prepareDisbursement(UUID loanId, LoanDisbursement.DisbursementMethod method,
                                                Map<String, String> methodDetails, String notes) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.ADMIN_APPROVED && loan.getStatus() != Loan.LoanStatus.TREASURER_DISBURSEMENT) {
            throw new RuntimeException("Loan must be approved before disbursement preparation.");
        }

        if (disbursementRepository.findByLoan(loan).isPresent()) {
            throw new RuntimeException("Disbursement already exists for this loan");
        }

        LoanDisbursement disbursement = LoanDisbursement.builder()
                .loan(loan)
                .member(loan.getMember())
                .amount(loan.getPrincipalAmount())
                .method(method)
                .status(LoanDisbursement.DisbursementStatus.PREPARED)
                .preparedBy(getCurrentUserId())
                .notes(notes)
                .build();

        LoanDisbursement saved = disbursementRepository.save(disbursement);

        loan.setStatus(Loan.LoanStatus.TREASURER_DISBURSEMENT);
        loanRepository.save(loan);

        return saved;
    }

    /**
     * CHAIRPERSON/ADMIN: Approve disbursement
     */
    public LoanDisbursement approveDisbursement(UUID disbursementId, String approvalNotes) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new RuntimeException("Disbursement not found"));

        if (disbursement.getStatus() != LoanDisbursement.DisbursementStatus.PREPARED) {
            throw new RuntimeException("Disbursement must be in PREPARED status for approval");
        }

        disbursement.setStatus(LoanDisbursement.DisbursementStatus.APPROVED);
        disbursement.setApprovedBy(getCurrentUserId());
        disbursement.setApprovedAt(LocalDateTime.now());
        if (approvalNotes != null) {
            disbursement.setNotes(disbursement.getNotes() + "\nApproval: " + approvalNotes);
        }

        return disbursementRepository.save(disbursement);
    }

    /**
     * TREASURER: Complete disbursement
     */
    public LoanDisbursement completeDisbursement(UUID disbursementId, String transactionReference,
                                                 String completionNotes, Map<String, String> additionalDetails) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new RuntimeException("Disbursement not found"));

        if (disbursement.getStatus() != LoanDisbursement.DisbursementStatus.APPROVED) {
            throw new RuntimeException("Disbursement must be approved before completion");
        }

        disbursement.setStatus(LoanDisbursement.DisbursementStatus.DISBURSED);
        disbursement.setDisbursedBy(getCurrentUserId());
        disbursement.setDisbursedAt(LocalDateTime.now());
        if (transactionReference != null) disbursement.setTransactionReference(transactionReference);

        LoanDisbursement saved = disbursementRepository.save(disbursement);

        // ✅ FIX: Use shared helper to set status to ACTIVE
        updateLoanStatusAndFinancials(disbursement.getLoan());

        // Accounting
        String sourceAccount = getCreditAccountForDisbursement(disbursement);
        accountingService.postLoanDisbursement(disbursement.getLoan(), sourceAccount);

        // Notify
        notificationService.notifyUser(disbursement.getMember().getId(), "Funds Disbursed",
                "Your loan has been disbursed and is now ACTIVE.", true, true);

        return saved;
    }

    // ✅ HELPER: Updates Loan to ACTIVE so it appears on Dashboard
    private void updateLoanStatusAndFinancials(Loan loan) {
        loan.setStatus(Loan.LoanStatus.ACTIVE); // ✅ CHANGED FROM DISBURSED TO ACTIVE
        loan.setDisbursementDate(LocalDate.now());
        loan.setLoanBalance(loan.getPrincipalAmount());

        int graceWeeks = 1;
        try {
            graceWeeks = Integer.parseInt(systemSettingService.getSetting("LOAN_GRACE_PERIOD_WEEKS").orElse("1"));
        } catch (Exception e) {
            log.warn("Using default grace period.");
        }
        loan.setGracePeriodWeeks(graceWeeks);
        repaymentService.generateSchedule(loan);
        loanRepository.save(loan);
    }

    public Map<String, Object> getDisbursementStatistics() {
        Map<String, Object> stats = new HashMap<>();
        for (LoanDisbursement.DisbursementStatus status : LoanDisbursement.DisbursementStatus.values()) {
            long count = disbursementRepository.countByStatus(status);
            stats.put(status.toString(), count);
        }
        return stats;
    }

    private String getCreditAccountForDisbursement(LoanDisbursement disbursement) {
        switch (disbursement.getMethod()) {
            case MPESA: return "1002";
            case CHEQUE:
            case BANK_TRANSFER: return "1010";
            case CASH: return "1001";
            default: return "1002";
        }
    }

    public LoanDisbursement getDisbursementByLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        return disbursementRepository.findByLoan(loan).orElse(null);
    }

    public List<LoanDisbursement> getPendingDisbursements() {
        return disbursementRepository.findByStatus(LoanDisbursement.DisbursementStatus.APPROVED);
    }

    public List<LoanDisbursement> getDisbursementsAwaitingApproval() {
        return disbursementRepository.findByStatus(LoanDisbursement.DisbursementStatus.PREPARED);
    }

    private UUID getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrOfficialEmail(username)
                .map(User::getId)
                .orElse(null);
    }
}