package com.sacco.sacco_system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sacco.sacco_system.entity.accounting.*;
import com.sacco.sacco_system.repository.accounting.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingService {

    private final GLAccountRepository accountRepository;
    private final JournalEntryRepository journalRepository;
    private final JournalLineRepository journalLineRepository;
    private final ObjectMapper objectMapper;

    /**
     * AUTO: Post a standard Double-Entry Transaction (2 legs)
     */
    @Transactional
    public void postDoubleEntry(String description, String referenceNo, String debitAccountCode, String creditAccountCode, BigDecimal amount) {

        GLAccount debitAcct = accountRepository.findById(debitAccountCode)
                .orElseThrow(() -> new RuntimeException("Debit Account not found: " + debitAccountCode));

        GLAccount creditAcct = accountRepository.findById(creditAccountCode)
                .orElseThrow(() -> new RuntimeException("Credit Account not found: " + creditAccountCode));

        JournalEntry entry = JournalEntry.builder()
                .transactionDate(LocalDateTime.now())
                .description(description)
                .referenceNo(referenceNo)
                .build();

        JournalLine debitLine = JournalLine.builder()
                .journalEntry(entry)
                .account(debitAcct)
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .build();

        JournalLine creditLine = JournalLine.builder()
                .journalEntry(entry)
                .account(creditAcct)
                .debit(BigDecimal.ZERO)
                .credit(amount)
                .build();

        entry.setLines(List.of(debitLine, creditLine));

        updateBalance(debitAcct, amount, true);
        updateBalance(creditAcct, amount, false);

        journalRepository.save(entry);
    }

    /**
     * MANUAL: Post a Multi-Line Journal Entry (Expenses, Assets, Adjustments)
     */
    @Transactional
    public void postManualJournalEntry(ManualEntryRequest request) {
        // 1. Validate Balance (Debits must equal Credits)
        BigDecimal totalDebit = request.getLines().stream()
                .map(ManualEntryLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = request.getLines().stream()
                .map(ManualEntryLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new RuntimeException("Journal Entry is unbalanced! Total Debit: " + totalDebit + ", Total Credit: " + totalCredit);
        }

        // 2. Create Header
        JournalEntry entry = JournalEntry.builder()
                .transactionDate(request.getDate().atStartOfDay()) // User selected date
                .postedDate(LocalDateTime.now())
                .description(request.getDescription())
                .referenceNo(request.getReference())
                .lines(new ArrayList<>())
                .build();

        // 3. Process Lines
        for (ManualEntryLine lineDto : request.getLines()) {
            GLAccount account = accountRepository.findById(lineDto.getAccountCode())
                    .orElseThrow(() -> new RuntimeException("Account not found: " + lineDto.getAccountCode()));

            JournalLine line = JournalLine.builder()
                    .journalEntry(entry)
                    .account(account)
                    .debit(lineDto.getDebit())
                    .credit(lineDto.getCredit())
                    .build();

            entry.getLines().add(line);

            // 4. Update Balances
            if (lineDto.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                updateBalance(account, lineDto.getDebit(), true);
            } else {
                updateBalance(account, lineDto.getCredit(), false);
            }
        }

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

    // --- HELPER DTOs ---
    @Data
    public static class ManualEntryRequest {
        private String description;
        private String reference;
        private LocalDate date;
        private List<ManualEntryLine> lines;
    }

    @Data
    public static class ManualEntryLine {
        private String accountCode;
        private BigDecimal debit = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;
    }

    // --- OTHER METHODS (Reports, Toggle, Create, Init) ---

    @Transactional
    public GLAccount toggleAccountStatus(String code) {
        GLAccount account = accountRepository.findById(code)
                .orElseThrow(() -> new RuntimeException("Account not found: " + code));
        account.setActive(!account.isActive());
        return accountRepository.save(account);
    }

    @Transactional
    public GLAccount createManualAccount(GLAccount account) {
        if (accountRepository.existsById(account.getCode())) {
            throw new RuntimeException("Account code " + account.getCode() + " already exists.");
        }
        account.setBalance(BigDecimal.ZERO);
        account.setActive(true);
        return accountRepository.save(account);
    }

    public List<GLAccount> getAccountsWithBalancesAsOf(LocalDate startDate, LocalDate endDate) {
        List<GLAccount> allAccounts = accountRepository.findAll();
        List<Object[]> totals;
        if (startDate == null) {
            totals = journalLineRepository.getAccountTotalsUpToDate(endDate.atTime(LocalTime.MAX));
        } else {
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