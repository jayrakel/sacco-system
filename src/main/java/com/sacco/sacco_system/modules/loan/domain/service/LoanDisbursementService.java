package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanDisbursement;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanDisbursementRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.SystemSettingService;
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

    // --- WRITE OPERATIONS ---

    public LoanDisbursement prepareDisbursement(UUID loanId, LoanDisbursement.DisbursementMethod method,
                                                Map<String, String> methodDetails, String notes) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.TREASURER_DISBURSEMENT) {
            throw new RuntimeException("Loan is not ready for disbursement. Current status: " + loan.getStatus());
        }

        if (disbursementRepository.findByLoan(loan).isPresent()) {
            throw new RuntimeException("Disbursement process already started for this loan.");
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

        mapMethodDetails(disbursement, method, methodDetails);

        return disbursementRepository.save(disbursement);
    }

    public LoanDisbursement completeDisbursement(UUID disbursementId, String transactionRef, String notes) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new RuntimeException("Disbursement not found"));

        disbursement.setStatus(LoanDisbursement.DisbursementStatus.DISBURSED);
        disbursement.setDisbursedBy(getCurrentUserId());
        disbursement.setDisbursedAt(LocalDateTime.now());
        disbursement.setTransactionReference(transactionRef);
        if (notes != null) disbursement.setNotes(disbursement.getNotes() + "\nCompleted: " + notes);

        LoanDisbursement saved = disbursementRepository.save(disbursement);
        Loan loan = disbursement.getLoan();

        // Critical State Transition: DISBURSED -> ACTIVE
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        loan.setDisbursementDate(LocalDate.now());
        loan.setLoanBalance(loan.getPrincipalAmount());

        repaymentService.generateSchedule(loan);
        loanRepository.save(loan);

        String sourceAccount = getCreditAccountForDisbursement(disbursement);
        accountingService.postEvent(
                "LOAN_DISBURSEMENT",
                "Disbursement: " + loan.getLoanNumber(),
                transactionRef,
                loan.getPrincipalAmount(),
                sourceAccount,
                "2001"
        );

        log.info("Loan {} Activated. Disbursement Complete.", loan.getLoanNumber());
        return saved;
    }

    // --- READ OPERATIONS ---

    public LoanDisbursement getDisbursementByLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        return disbursementRepository.findByLoan(loan).orElse(null);
    }

    public List<LoanDisbursement> getPendingDisbursements() {
        // Items that are prepared but not yet completed
        return disbursementRepository.findByStatus(LoanDisbursement.DisbursementStatus.PREPARED);
    }

    // ✅ FIXED: Added this method to satisfy the controller
    // In the new flow, "Awaiting Approval" effectively means "Prepared, waiting for completion"
    public List<LoanDisbursement> getDisbursementsAwaitingApproval() {
        return disbursementRepository.findByStatus(LoanDisbursement.DisbursementStatus.PREPARED);
    }

    // ✅ FIXED: Added this method for the dashboard
    public Map<String, Object> getDisbursementStatistics() {
        Map<String, Object> stats = new HashMap<>();
        for (LoanDisbursement.DisbursementStatus status : LoanDisbursement.DisbursementStatus.values()) {
            long count = disbursementRepository.countByStatus(status);
            stats.put(status.toString(), count);
        }
        return stats;
    }

    // --- HELPERS ---

    private void mapMethodDetails(LoanDisbursement d, LoanDisbursement.DisbursementMethod method, Map<String, String> details) {
        switch (method) {
            case CHEQUE:
                d.setChequeNumber(details.get("chequeNumber"));
                d.setBankName(details.get("bankName"));
                d.setPayableTo(details.get("payableTo"));
                break;
            case MPESA:
                d.setMpesaPhoneNumber(details.get("phoneNumber"));
                break;
            case BANK_TRANSFER:
                d.setAccountNumber(details.get("accountNumber"));
                d.setBankCode(details.get("bankCode"));
                break;
        }
    }

    private String getCreditAccountForDisbursement(LoanDisbursement d) {
        switch (d.getMethod()) {
            case MPESA: return "1002";
            case CASH: return "1001";
            default: return "1010";
        }
    }

    private UUID getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrOfficialEmail(username).map(User::getId).orElse(null);
    }
}