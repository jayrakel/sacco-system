package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanRepaymentService {

    private final LoanRepository loanRepository;
    private final AccountingService accountingService;
    private final TransactionRepository transactionRepository;
    private final ReferenceCodeService referenceCodeService;

    /**
     * Process a loan repayment from the Deposit Service (or other sources)
     */
    @Transactional
    public void processPayment(Loan loan, BigDecimal amount, String sourceAccountCode) {
        // 1. Update Balance
        // Using the new clean Loan entity fields
        BigDecimal currentBalance = loan.getTotalOutstandingAmount();
        BigDecimal newBalance = currentBalance.subtract(amount);

        // Prevent negative balance (optional logic, but good for data integrity)
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }

        loan.setTotalOutstandingAmount(newBalance);

        // 2. Auto-Complete if paid off
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setLoanStatus(Loan.LoanStatus.CLOSED);
        }

        loanRepository.save(loan);

        // 3. Accounting Entry
        // We link the repayment to the GL Account defined in the Loan Product
        String receivableAccount = loan.getProduct().getReceivableAccountCode();
        if (receivableAccount == null) receivableAccount = "1201"; // Default Asset Account

        // Post Event: Credit Loan Receivable (Asset), Debit Source (Cash/Bank)
        accountingService.postEvent(
                "LOAN_REPAYMENT",
                "Repayment - " + loan.getLoanNumber(),
                loan.getLoanNumber(),
                amount,
                sourceAccountCode
        );

        // 4. Create Transaction Record
        Transaction txn = Transaction.builder()
                .member(loan.getMember())
                .type(Transaction.TransactionType.LOAN_REPAYMENT)
                .amount(amount)
                .referenceCode(referenceCodeService.generateReferenceCode())
                .description("Repayment for Loan " + loan.getLoanNumber())
                .balanceAfter(newBalance)
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(txn);

        log.info("Processed repayment of {} for loan {}. New Balance: {}", amount, loan.getLoanNumber(), newBalance);
    }
}