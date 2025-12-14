package com.sacco.sacco_system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sacco.sacco_system.entity.accounting.*;
import com.sacco.sacco_system.repository.accounting.GLAccountRepository;
import com.sacco.sacco_system.repository.accounting.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingService {

    private final GLAccountRepository accountRepository;
    private final JournalEntryRepository journalRepository;
    private final ObjectMapper objectMapper; // Spring Boot automatically provides this

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
        if (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE) {
            account.setBalance(isDebit ? account.getBalance().add(amount) : account.getBalance().subtract(amount));
        } else {
            account.setBalance(isDebit ? account.getBalance().subtract(amount) : account.getBalance().add(amount));
        }
        accountRepository.save(account);
    }

    // ✅ NEW: Dynamic Initialization from JSON
    public void initChartOfAccounts() {
        if(accountRepository.count() == 0) {
            try {
                System.out.println("📂 Loading Chart of Accounts from JSON...");

                // Read the file
                InputStream inputStream = new ClassPathResource("accounts.json").getInputStream();
                List<GLAccount> accounts = objectMapper.readValue(inputStream, new TypeReference<List<GLAccount>>(){});

                // Save each account
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