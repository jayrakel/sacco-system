package com.sacco.sacco_system.service;

import com.sacco.sacco_system.entity.accounting.*;
import com.sacco.sacco_system.repository.accounting.GLAccountRepository;
import com.sacco.sacco_system.repository.accounting.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingService {

    private final GLAccountRepository accountRepository;
    private final JournalEntryRepository journalRepository;

    /**
     * POSTS A DOUBLE-ENTRY TRANSACTION TO THE GL
     * @param description Narrative
     * @param referenceNo External Ref (TRX ID)
     * @param debitAccountCode Code of account receiving value
     * @param creditAccountCode Code of account giving value
     * @param amount The value
     */
    @Transactional
    public void postDoubleEntry(String description, String referenceNo, String debitAccountCode, String creditAccountCode, BigDecimal amount) {

        GLAccount debitAcct = accountRepository.findById(debitAccountCode)
                .orElseThrow(() -> new RuntimeException("Debit Account not found: " + debitAccountCode));

        GLAccount creditAcct = accountRepository.findById(creditAccountCode)
                .orElseThrow(() -> new RuntimeException("Credit Account not found: " + creditAccountCode));

        // 1. Create Header
        JournalEntry entry = JournalEntry.builder()
                .transactionDate(LocalDateTime.now())
                .description(description)
                .referenceNo(referenceNo)
                .build();

        // 2. Create Debit Line
        JournalLine debitLine = JournalLine.builder()
                .journalEntry(entry)
                .account(debitAcct)
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .build();

        // 3. Create Credit Line
        JournalLine creditLine = JournalLine.builder()
                .journalEntry(entry)
                .account(creditAcct)
                .debit(BigDecimal.ZERO)
                .credit(amount)
                .build();

        entry.setLines(List.of(debitLine, creditLine));

        // 4. Update GL Balances (Simple rule: Asset/Expense increases on Debit, Liability/Income increases on Credit)
        updateBalance(debitAcct, amount, true);
        updateBalance(creditAcct, amount, false);

        journalRepository.save(entry);
    }

    private void updateBalance(GLAccount account, BigDecimal amount, boolean isDebit) {
        // Standard Accounting Equation Logic
        if (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE) {
            account.setBalance(isDebit ? account.getBalance().add(amount) : account.getBalance().subtract(amount));
        } else {
            // Liability, Equity, Income -> Credit increases balance
            account.setBalance(isDebit ? account.getBalance().subtract(amount) : account.getBalance().add(amount));
        }
        accountRepository.save(account);
    }

    // Helper to Initialize Default Accounts
    public void initChartOfAccounts() {
        if(accountRepository.count() == 0) {
            // Assets
            createAccount("1001", "Cash on Hand", AccountType.ASSET);
            createAccount("1002", "Bank - Equity Bank", AccountType.ASSET);
            createAccount("1200", "Loans Receivable", AccountType.ASSET);

            // Liabilities
            createAccount("2001", "Member Savings Control", AccountType.LIABILITY);
            createAccount("2002", "Suspense Account", AccountType.LIABILITY);

            // Income
            createAccount("4001", "Registration Fees", AccountType.INCOME);
            createAccount("4002", "Loan Interest Income", AccountType.INCOME);
            createAccount("4003", "Fines & Penalties", AccountType.INCOME);

            // Expenses
            createAccount("5001", "Office Expenses", AccountType.EXPENSE);
        }
    }

    private void createAccount(String code, String name, AccountType type) {
        accountRepository.save(GLAccount.builder().code(code).name(name).type(type).balance(BigDecimal.ZERO).active(true).build());
    }
}