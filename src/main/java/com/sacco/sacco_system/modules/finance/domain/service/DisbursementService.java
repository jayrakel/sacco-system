package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.loan.domain.service.LoanAmortizationService; // ✅ IMPORT ADDED
import com.sacco.sacco_system.modules.loan.domain.service.GuarantorService;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisbursementService {

    private final LoanRepository loanRepository;
    private final TransactionRepository transactionRepository;
    private final AccountingService accountingService;
    private final LoanAmortizationService loanAmortizationService; // ✅ INJECTED SERVICE
    private final GuarantorService guarantorService;

    /**
     * Get loans awaiting disbursement (APPROVED_BY_COMMITTEE status)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLoansAwaitingDisbursement() {
        List<Loan> loans = loanRepository.findByLoanStatus(Loan.LoanStatus.APPROVED_BY_COMMITTEE);

        return loans.stream()
                .map(loan -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", loan.getId());
                    data.put("loanNumber", loan.getLoanNumber());
                    data.put("memberName", loan.getMember().getFirstName() + " " + loan.getMember().getLastName());
                    data.put("memberNumber", loan.getMember().getMemberNumber());
                    data.put("memberPhone", loan.getMember().getPhoneNumber());
                    data.put("productName", loan.getProduct().getProductName());
                    data.put("approvedAmount", loan.getApprovedAmount());
                    data.put("durationWeeks", loan.getDurationWeeks());
                    data.put("approvalDate", loan.getApprovalDate());
                    data.put("loanStatus", loan.getLoanStatus().name());
                    return data;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get disbursed loans
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDisbursedLoans() {
        List<Loan> loans = loanRepository.findByLoanStatusIn(
                List.of(Loan.LoanStatus.DISBURSED, Loan.LoanStatus.ACTIVE)
        );

        return loans.stream()
                .map(loan -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", loan.getId());
                    data.put("loanNumber", loan.getLoanNumber());
                    data.put("memberName", loan.getMember().getFirstName() + " " + loan.getMember().getLastName());
                    data.put("memberNumber", loan.getMember().getMemberNumber());
                    data.put("productName", loan.getProduct().getProductName());
                    data.put("disbursedAmount", loan.getDisbursedAmount());
                    data.put("disbursementDate", loan.getDisbursementDate());
                    data.put("loanStatus", loan.getLoanStatus().name());
                    return data;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get finance statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFinanceStatistics() {
        List<Loan> pending = loanRepository.findByLoanStatus(Loan.LoanStatus.APPROVED_BY_COMMITTEE);
        List<Loan> disbursed = loanRepository.findByLoanStatusIn(
                List.of(Loan.LoanStatus.DISBURSED, Loan.LoanStatus.ACTIVE)
        );

        BigDecimal pendingAmount = pending.stream()
                .map(Loan::getApprovedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal disbursedAmount = disbursed.stream()
                .map(Loan::getDisbursedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Today's disbursements
        LocalDate today = LocalDate.now();
        List<Loan> todayDisbursed = disbursed.stream()
                .filter(loan -> loan.getDisbursementDate() != null && loan.getDisbursementDate().equals(today))
                .toList();

        BigDecimal todayAmount = todayDisbursed.stream()
                .map(Loan::getDisbursedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingCount", pending.size());
        stats.put("pendingAmount", pendingAmount);
        stats.put("disbursedCount", disbursed.size());
        stats.put("disbursedAmount", disbursedAmount);
        stats.put("todayDisbursed", todayDisbursed.size());
        stats.put("todayAmount", todayAmount);

        return stats;
    }

    /**
     * Disburse a loan
     */
    @Transactional
    public void disburseLoan(UUID loanId, String disbursementMethod, String phoneNumber, String reference, String disbursedBy) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

        if (loan.getLoanStatus() != Loan.LoanStatus.APPROVED_BY_COMMITTEE) {
            throw new ApiException("Loan is not approved for disbursement", 400);
        }

        // Calculate interest based on product interest type
        BigDecimal principal = loan.getApprovedAmount();
        BigDecimal interestRate = loan.getInterestRate();
        Integer durationWeeks = loan.getDurationWeeks();

        // Calculate total interest (FLAT rate for now - can be extended for REDUCING_BALANCE)
        // Formula: (Principal × Rate × Duration) / 100
        // For weekly loans: (Principal × Annual Rate × Weeks) / (100 × 52)
        BigDecimal totalInterest = principal
                .multiply(interestRate)
                .multiply(BigDecimal.valueOf(durationWeeks))
                .divide(BigDecimal.valueOf(5200), 2, BigDecimal.ROUND_HALF_UP); // 100 × 52 weeks

        BigDecimal totalRepayable = principal.add(totalInterest);

        // Calculate weekly repayment amount
        BigDecimal weeklyRepayment = totalRepayable
                .divide(BigDecimal.valueOf(durationWeeks), 2, BigDecimal.ROUND_HALF_UP);

        // Calculate maturity date (disbursement date + duration in weeks)
        LocalDate maturityDate = LocalDate.now().plusWeeks(durationWeeks);

        // Update loan with all required fields
        loan.setDisbursedAmount(principal);
        loan.setDisbursementDate(LocalDate.now());
        loan.setLoanStatus(Loan.LoanStatus.DISBURSED);
        loan.setActive(true);

        // ✅ Set outstanding amounts (initially equals total repayable)
        loan.setOutstandingPrincipal(principal);
        loan.setOutstandingInterest(totalInterest);
        loan.setTotalOutstandingAmount(totalRepayable);

        // ✅ Set repayment schedule details
        loan.setWeeklyRepaymentAmount(weeklyRepayment);
        loan.setMaturityDate(maturityDate);

        // ✅ Set audit fields
        loan.setUpdatedBy(disbursedBy);
        if (loan.getCreatedBy() == null) {
            loan.setCreatedBy(disbursedBy);
        }

        loanRepository.save(loan);

        // ✅ NEW: Generate Amortization Schedule (Installments)
        // This ensures the LoanDailyProcessor can track arrears properly
        loanAmortizationService.generateSchedule(loan);

        guarantorService.lockGuarantorFunds(loan);

        // Create disbursement transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN" + System.currentTimeMillis());
        transaction.setLoan(loan);
        transaction.setType(Transaction.TransactionType.LOAN_DISBURSEMENT);
        transaction.setAmount(loan.getDisbursedAmount());
        transaction.setDescription("Loan disbursement - " + loan.getLoanNumber());

        // Convert string to PaymentMethod enum
        Transaction.PaymentMethod paymentMethodEnum;
        try {
            paymentMethodEnum = Transaction.PaymentMethod.valueOf(disbursementMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default to BANK if invalid method provided
            paymentMethodEnum = Transaction.PaymentMethod.BANK;
        }
        transaction.setPaymentMethod(paymentMethodEnum);

        transaction.setReferenceCode(reference);
        transaction.setExternalReference(phoneNumber);
        transaction.setBalanceAfter(loan.getDisbursedAmount());
        transaction.setTransactionDate(LocalDateTime.now());

        transactionRepository.save(transaction);

        // ✅ POST TO GENERAL LEDGER (Double-Entry Accounting)
        try {
            // Map payment method to GL account code (source of funds)
            String sourceAccount = mapPaymentMethodToGLAccount(disbursementMethod);

            // Post loan disbursement to GL
            // This creates:
            // DR: 1200 (Loans Receivable) - Asset increases
            // CR: 1002/1001/1010 (Cash/M-Pesa/Bank) - Asset decreases
            accountingService.postLoanDisbursement(loan, sourceAccount);

            log.info("✅ GL Entry Posted: Loan {} disbursement from account {}",
                    loan.getLoanNumber(), sourceAccount);
        } catch (Exception e) {
            log.error("❌ Failed to post GL entry for loan {}: {}",
                    loan.getLoanNumber(), e.getMessage(), e);
            // Note: Transaction is already saved, GL posting failure is logged
            // Can be corrected manually or via migration script
        }

        log.info("✅ Loan {} disbursed: {} to {} via {}. Principal: {}, Interest: {}, Total: {}, Weekly: {}, Maturity: {}",
                loan.getLoanNumber(),
                loan.getDisbursedAmount(),
                loan.getMember().getFirstName() + " " + loan.getMember().getLastName(),
                disbursementMethod,
                principal,
                totalInterest,
                totalRepayable,
                weeklyRepayment,
                maturityDate);
    }

    /**
     * Map payment method to GL account code
     * Used to determine which account to credit when disbursing loans
     */
    private String mapPaymentMethodToGLAccount(String paymentMethod) {
        return switch (paymentMethod.toUpperCase()) {
            case "MPESA" -> "1002";  // M-Pesa Account
            case "CASH" -> "1001";   // Cash Account
            case "BANK" -> "1010";   // Bank Account
            default -> "1002";       // Default to M-Pesa
        };
    }

    /**
     * ✅ MIGRATION HELPER: Recalculate outstanding amounts for existing disbursed loans
     * This fixes loans that were disbursed before the calculation logic was added
     */
    @Transactional
    public Map<String, Object> recalculateExistingLoans() {
        List<Loan> disbursedLoans = loanRepository.findByLoanStatusIn(
                List.of(Loan.LoanStatus.DISBURSED, Loan.LoanStatus.ACTIVE)
        );

        int updated = 0;
        int skipped = 0;

        for (Loan loan : disbursedLoans) {
            // Check if loan needs recalculation
            if (loan.getTotalOutstandingAmount() == null ||
                    loan.getTotalOutstandingAmount().compareTo(BigDecimal.ZERO) == 0 ||
                    loan.getWeeklyRepaymentAmount() == null) {

                BigDecimal principal = loan.getDisbursedAmount() != null ?
                        loan.getDisbursedAmount() : loan.getApprovedAmount();
                BigDecimal interestRate = loan.getInterestRate();
                Integer durationWeeks = loan.getDurationWeeks();

                // Calculate total interest (same formula as disbursement)
                BigDecimal totalInterest = principal
                        .multiply(interestRate)
                        .multiply(BigDecimal.valueOf(durationWeeks))
                        .divide(BigDecimal.valueOf(5200), 2, BigDecimal.ROUND_HALF_UP);

                BigDecimal totalRepayable = principal.add(totalInterest);
                BigDecimal weeklyRepayment = totalRepayable
                        .divide(BigDecimal.valueOf(durationWeeks), 2, BigDecimal.ROUND_HALF_UP);

                // Update loan
                loan.setOutstandingPrincipal(principal);
                loan.setOutstandingInterest(totalInterest);
                loan.setTotalOutstandingAmount(totalRepayable);
                loan.setWeeklyRepaymentAmount(weeklyRepayment);

                // Set maturity date if missing
                if (loan.getMaturityDate() == null && loan.getDisbursementDate() != null) {
                    loan.setMaturityDate(loan.getDisbursementDate().plusWeeks(durationWeeks));
                }

                loanRepository.save(loan);
                updated++;

                log.info("✅ Recalculated loan {}: Principal={}, Interest={}, Total={}, Weekly={}",
                        loan.getLoanNumber(), principal, totalInterest, totalRepayable, weeklyRepayment);
            } else {
                skipped++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalLoans", disbursedLoans.size());
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("message", String.format("Updated %d loans, skipped %d loans", updated, skipped));

        return result;
    }
}