package com.sacco.sacco_system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sacco.sacco_system.entity.accounting.*;
import com.sacco.sacco_system.repository.accounting.GLAccountRepository;
import com.sacco.sacco_system.repository.accounting.JournalEntryRepository;
import com.sacco.sacco_system.repository.accounting.JournalLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingService {

    private final GLAccountRepository accountRepository;
    private final JournalEntryRepository journalRepository;
    private final JournalLineRepository journalLineRepository;
    private final ObjectMapper objectMapper;

    /**
     * POSTS A DOUBLE-ENTRY TRANSACTION TO THE GL
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

        // 4. Update GL Balances
        updateBalance(debitAcct, amount, true);
        updateBalance(creditAcct, amount, false);

        journalRepository.save(entry);
    }

    private void updateBalance(GLAccount account, BigDecimal amount, boolean isDebit) {
        // Asset/Expense increase on Debit. Liability/Equity/Income increase on Credit.
        if (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE) {
            account.setBalance(isDebit ? account.getBalance().add(amount) : account.getBalance().subtract(amount));
        } else {
            account.setBalance(isDebit ? account.getBalance().subtract(amount) : account.getBalance().add(amount));
        }
        accountRepository.save(account);
    }

    // Toggle Account Status
    @Transactional
    public GLAccount toggleAccountStatus(String code) {
        GLAccount account = accountRepository.findById(code)
                .orElseThrow(() -> new RuntimeException("Account not found: " + code));

        account.setActive(!account.isActive());
        return accountRepository.save(account);
    }

    // Create Manual Account
    @Transactional
    public GLAccount createManualAccount(GLAccount account) {
        if (accountRepository.existsById(account.getCode())) {
            throw new RuntimeException("Account code " + account.getCode() + " already exists.");
        }
        account.setBalance(BigDecimal.ZERO);
        account.setActive(true);
        return accountRepository.save(account);
    }

    // Generate Report Data (Dynamic Balances based on Date)
    public List<GLAccount> getAccountsWithBalancesAsOf(LocalDate startDate, LocalDate endDate) {
        List<GLAccount> allAccounts = accountRepository.findAll();

        List<Object[]> totals;
        if (startDate == null) {
            // Balance Sheet Mode (Up to End Date)
            totals = journalLineRepository.getAccountTotalsUpToDate(endDate.atTime(LocalTime.MAX));
        } else {
            // Income Statement Mode (Range)
            totals = journalLineRepository.getAccountTotalsInRange(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        }

        for (GLAccount account : allAccounts) {
            BigDecimal debitSum = BigDecimal.ZERO;
            BigDecimal creditSum = BigDecimal.ZERO;

            for (Object[] row : totals) {
                if (row[0].equals(account.getCode())) {
                    debitSum = (row[1] != null) ? (BigDecimal) row[1] : BigDecimal.ZERO;
                    creditSum = (row[2] != null) ? (BigDecimal) row[2] : BigDecimal.ZERO;
                    break;
                }
            }

            BigDecimal netBalance;
            if (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE) {
                netBalance = debitSum.subtract(creditSum);
            } else {
                netBalance = creditSum.subtract(debitSum);
            }

            account.setBalance(netBalance);
        }

        return allAccounts;
    }

    // Initialize Default Accounts
    public void initChartOfAccounts() {
        if(accountRepository.count() == 0) {
            try {
                System.out.println("📂 Loading Chart of Accounts from JSON...");
                InputStream inputStream = new ClassPathResource("accounts.json").getInputStream();
                List<GLAccount> accounts = objectMapper.readValue(inputStream, new TypeReference<List<GLAccount>>(){});

                for (GLAccount account : accounts) {
                    account.setBalance(BigDecimal.ZERO);
                    account.setActive(true);
                    accountRepository.save(account);
                }
                System.out.println("✅ Successfully initialized " + accounts.size() + " GL Accounts.");
            } catch (Exception e) {
                System.err.println("❌ Failed to load accounts.json: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}