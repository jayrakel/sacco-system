package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.loan.domain.service.LoanAmortizationService;
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
    private final LoanAmortizationService loanAmortizationService;
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

        // ✅ Set outstanding amounts
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

        // ✅ POST TO GENERAL LEDGER
        try {
            String sourceAccount = mapPaymentMethodToGLAccount(disbursementMethod);
            accountingService.postLoanDisbursement(loan, sourceAccount);
            log.info("✅ GL Entry Posted: Loan {} disbursement from account {}",
                    loan.getLoanNumber(), sourceAccount);
        } catch (Exception e) {
            log.error("❌ Failed to post GL entry for loan {}: {}",
                    loan.getLoanNumber(), e.getMessage(), e);
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
     * ✅ MIGRATION HELPER: Recalculate totals AND Generate Schedules for existing loans
     */
    @Transactional
    public Map<String, Object> recalculateExistingLoans() {
        List<Loan> disbursedLoans = loanRepository.findByLoanStatusIn(
                List.of(Loan.LoanStatus.DISBURSED, Loan.LoanStatus.ACTIVE)
        );

        int updated = 0;

        for (Loan loan : disbursedLoans) {
            // 1. Recalculate amounts if missing (Safety check)
            if (loan.getTotalOutstandingAmount() == null ||
                    loan.getTotalOutstandingAmount().compareTo(BigDecimal.ZERO) == 0 ||
                    loan.getWeeklyRepaymentAmount() == null) {

                BigDecimal principal = loan.getDisbursedAmount() != null ?
                        loan.getDisbursedAmount() : loan.getApprovedAmount();
                BigDecimal interestRate = loan.getInterestRate();
                Integer durationWeeks = loan.getDurationWeeks();

                BigDecimal totalInterest = principal
                        .multiply(interestRate)
                        .multiply(BigDecimal.valueOf(durationWeeks))
                        .divide(BigDecimal.valueOf(5200), 2, BigDecimal.ROUND_HALF_UP);

                BigDecimal totalRepayable = principal.add(totalInterest);
                BigDecimal weeklyRepayment = totalRepayable
                        .divide(BigDecimal.valueOf(durationWeeks), 2, BigDecimal.ROUND_HALF_UP);

                loan.setOutstandingPrincipal(principal);
                loan.setOutstandingInterest(totalInterest);
                loan.setTotalOutstandingAmount(totalRepayable);
                loan.setWeeklyRepaymentAmount(weeklyRepayment);

                if (loan.getMaturityDate() == null && loan.getDisbursementDate() != null) {
                    loan.setMaturityDate(loan.getDisbursementDate().plusWeeks(durationWeeks));
                }

                loanRepository.save(loan);
                log.info("✅ Recalculated totals for loan {}", loan.getLoanNumber());
            }

            // 2. ✅ FORCE GENERATE SCHEDULE FOR ALL EXISTING LOANS
            // This is the key fix for "Schedule Generated" issue
            loanAmortizationService.generateSchedule(loan);

            updated++;
            log.info("✅ Generated missing schedule for loan {}", loan.getLoanNumber());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalLoans", disbursedLoans.size());
        result.put("fixed", updated);
        result.put("message", "Successfully recalculated totals and generated schedules for all active loans.");

        return result;
    }
}