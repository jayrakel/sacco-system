package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanDisbursement;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanDisbursementRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    /**
     * TREASURER: Prepare disbursement (write cheque, prepare transfer, etc.)
     */
    public LoanDisbursement prepareDisbursement(UUID loanId, LoanDisbursement.DisbursementMethod method,
                                                 Map<String, String> methodDetails, String notes) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Validate loan status
        if (loan.getStatus() != Loan.LoanStatus.ADMIN_APPROVED) {
            throw new RuntimeException("Loan must be approved before disbursement preparation. Current status: " + loan.getStatus());
        }

        // Check if disbursement already exists
        if (disbursementRepository.findByLoan(loan).isPresent()) {
            throw new RuntimeException("Disbursement already exists for this loan");
        }

        // Create disbursement record
        LoanDisbursement disbursement = LoanDisbursement.builder()
                .loan(loan)
                .member(loan.getMember())
                .amount(loan.getPrincipalAmount())
                .method(method)
                .status(LoanDisbursement.DisbursementStatus.PREPARED)
                .preparedBy(getCurrentUserId())
                .notes(notes)
                .build();

        // Set method-specific details
        switch (method) {
            case CHEQUE:
                disbursement.setChequeNumber(methodDetails.get("chequeNumber"));
                disbursement.setBankName(methodDetails.get("bankName"));
                disbursement.setChequeDate(LocalDate.parse(methodDetails.get("chequeDate")));
                disbursement.setPayableTo(methodDetails.getOrDefault("payableTo", loan.getMemberName()));
                break;

            case BANK_TRANSFER:
            case EFT:
            case RTGS:
                disbursement.setAccountNumber(methodDetails.get("accountNumber"));
                disbursement.setAccountName(methodDetails.get("accountName"));
                disbursement.setBankCode(methodDetails.get("bankCode"));
                break;

            case MPESA:
                disbursement.setMpesaPhoneNumber(methodDetails.get("phoneNumber"));
                break;

            case CASH:
                // Cash details filled during disbursement
                break;
        }

        LoanDisbursement saved = disbursementRepository.save(disbursement);

        // Update loan status
        loan.setStatus(Loan.LoanStatus.TREASURER_DISBURSEMENT);
        loanRepository.save(loan);

        log.info("Disbursement prepared for loan {} via {}", loan.getLoanNumber(), method);

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

        log.info("Disbursement {} approved", disbursement.getDisbursementNumber());

        return disbursementRepository.save(disbursement);
    }

    /**
     * TREASURER: Complete disbursement (mark as disbursed/collected)
     */
    public LoanDisbursement completeDisbursement(UUID disbursementId, String transactionReference,
                                                  String completionNotes, Map<String, String> additionalDetails) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new RuntimeException("Disbursement not found"));

        if (disbursement.getStatus() != LoanDisbursement.DisbursementStatus.APPROVED) {
            throw new RuntimeException("Disbursement must be approved before completion");
        }

        // Update based on method
        switch (disbursement.getMethod()) {
            case CHEQUE:
                disbursement.setStatus(LoanDisbursement.DisbursementStatus.COLLECTED);
                disbursement.setTransactionReference(transactionReference != null ?
                        transactionReference : disbursement.getChequeNumber());
                break;

            case BANK_TRANSFER:
            case EFT:
            case RTGS:
                disbursement.setStatus(LoanDisbursement.DisbursementStatus.DISBURSED);
                disbursement.setTransactionReference(transactionReference);
                break;

            case MPESA:
                disbursement.setStatus(LoanDisbursement.DisbursementStatus.DISBURSED);
                disbursement.setMpesaTransactionId(transactionReference);
                break;

            case CASH:
                disbursement.setStatus(LoanDisbursement.DisbursementStatus.DISBURSED);
                if (additionalDetails != null) {
                    disbursement.setReceivedBy(additionalDetails.get("receivedBy"));
                    disbursement.setWitnessedBy(additionalDetails.get("witnessedBy"));
                }
                break;
        }

        disbursement.setDisbursedBy(getCurrentUserId());
        disbursement.setDisbursedAt(LocalDateTime.now());
        if (completionNotes != null) {
            disbursement.setNotes(disbursement.getNotes() + "\nDisbursement: " + completionNotes);
        }

        LoanDisbursement saved = disbursementRepository.save(disbursement);

        // Update loan status
        Loan loan = disbursement.getLoan();
        loan.setStatus(Loan.LoanStatus.DISBURSED);
        loan.setDisbursementDate(LocalDate.now());
        loan.setLoanBalance(loan.getPrincipalAmount()); // Set initial balance
        loanRepository.save(loan);

        // ✅ POST TO ACCOUNTING - Creates: DEBIT Loans Receivable (1100), CREDIT Cash (1020)
        accountingService.postLoanDisbursement(loan);

        log.info("Disbursement completed for loan {}. Method: {}. Accounting entry created.",
                loan.getLoanNumber(), disbursement.getMethod());

        return saved;
    }

    /**
     * TREASURER: Mark cheque as cleared
     */
    public LoanDisbursement markChequeCleared(UUID disbursementId) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new RuntimeException("Disbursement not found"));

        if (disbursement.getMethod() != LoanDisbursement.DisbursementMethod.CHEQUE) {
            throw new RuntimeException("Only cheque disbursements can be marked as cleared");
        }

        if (disbursement.getStatus() != LoanDisbursement.DisbursementStatus.COLLECTED) {
            throw new RuntimeException("Cheque must be collected before it can be marked as cleared");
        }

        disbursement.setStatus(LoanDisbursement.DisbursementStatus.CLEARED);

        log.info("Cheque {} cleared", disbursement.getChequeNumber());

        return disbursementRepository.save(disbursement);
    }

    /**
     * TREASURER: Mark cheque as bounced
     */
    public LoanDisbursement markChequeBounced(UUID disbursementId, String reason) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new RuntimeException("Disbursement not found"));

        if (disbursement.getMethod() != LoanDisbursement.DisbursementMethod.CHEQUE) {
            throw new RuntimeException("Only cheque disbursements can bounce");
        }

        disbursement.setStatus(LoanDisbursement.DisbursementStatus.BOUNCED);
        disbursement.setNotes(disbursement.getNotes() + "\nBounced: " + reason);

        // Update loan status back
        Loan loan = disbursement.getLoan();
        loan.setStatus(Loan.LoanStatus.TREASURER_DISBURSEMENT); // Need to re-disburse
        loanRepository.save(loan);

        log.warn("Cheque {} bounced. Reason: {}", disbursement.getChequeNumber(), reason);

        return disbursementRepository.save(disbursement);
    }

    /**
     * Get disbursement by loan
     */
    public LoanDisbursement getDisbursementByLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        return disbursementRepository.findByLoan(loan)
                .orElse(null);
    }

    /**
     * Get pending disbursements (waiting for treasurer)
     */
    public List<LoanDisbursement> getPendingDisbursements() {
        return disbursementRepository.findByStatus(LoanDisbursement.DisbursementStatus.APPROVED);
    }

    /**
     * Get disbursements awaiting approval
     */
    public List<LoanDisbursement> getDisbursementsAwaitingApproval() {
        return disbursementRepository.findByStatus(LoanDisbursement.DisbursementStatus.PREPARED);
    }

    /**
     * Get disbursement statistics
     */
    public Map<String, Object> getDisbursementStatistics() {
        Map<String, Object> stats = new HashMap<>();

        for (LoanDisbursement.DisbursementStatus status : LoanDisbursement.DisbursementStatus.values()) {
            long count = disbursementRepository.countByStatus(status);
            stats.put(status.toString(), count);
        }

        return stats;
    }

    private UUID getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrOfficialEmail(username)
                .map(User::getId)
                .orElse(null);
    }
}

