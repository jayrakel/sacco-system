package com.sacco.sacco_system.service;

import com.sacco.sacco_system.entity.GLAccount;
import com.sacco.sacco_system.entity.JournalEntry;
import com.sacco.sacco_system.entity.JournalEntryLine;
import com.sacco.sacco_system.repository.GLAccountRepository;
import com.sacco.sacco_system.repository.JournalEntryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingService {

    private final GLAccountRepository glAccountRepository;
    private final JournalEntryRepository journalEntryRepository;

    // --- 1. Seed Default Chart of Accounts ---
    @PostConstruct
    public void initCoA() {
        createAccountIfMissing("1000", "Cash / Bank", GLAccount.AccountType.ASSET);
        createAccountIfMissing("1200", "Loans Receivable", GLAccount.AccountType.ASSET);
        createAccountIfMissing("2000", "Member Savings", GLAccount.AccountType.LIABILITY);
        createAccountIfMissing("2100", "Share Capital", GLAccount.AccountType.EQUITY);
        createAccountIfMissing("4000", "Interest Income", GLAccount.AccountType.INCOME);
        createAccountIfMissing("4100", "Fee Income", GLAccount.AccountType.INCOME);
    }

    private void createAccountIfMissing(String code, String name, GLAccount.AccountType type) {
        if (glAccountRepository.findByCode(code).isEmpty()) {
            glAccountRepository.save(GLAccount.builder()
                    .code(code)
                    .name(name)
                    .type(type)
                    .balance(BigDecimal.ZERO)
                    .build());
        }
    }

    // --- 2. Core Posting Logic ---
    @Transactional
    public void postJournalEntry(String description, String reference, List<EntryRequest> entries) {
        // Validation: Debits must equal Credits
        BigDecimal totalDebit = entries.stream().map(e -> e.debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entries.stream().map(e -> e.credit).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new RuntimeException("Accounting Error: Debits (" + totalDebit + ") do not equal Credits (" + totalCredit + ")");
        }

        JournalEntry journal = JournalEntry.builder()
                .description(description)
                .reference(reference)
                .build();

        List<JournalEntryLine> lines = new ArrayList<>();

        for (EntryRequest req : entries) {
            GLAccount account = glAccountRepository.findByCode(req.accountCode)
                    .orElseThrow(() -> new RuntimeException("GL Account not found: " + req.accountCode));

            // Update GL Account Balance
            // Asset/Expense: Debit increases, Credit decreases
            // Liability/Equity/Income: Credit increases, Debit decreases
            if (account.getType() == GLAccount.AccountType.ASSET || account.getType() == GLAccount.AccountType.EXPENSE) {
                account.setBalance(account.getBalance().add(req.debit).subtract(req.credit));
            } else {
                account.setBalance(account.getBalance().add(req.credit).subtract(req.debit));
            }
            glAccountRepository.save(account);

            JournalEntryLine line = JournalEntryLine.builder()
                    .journalEntry(journal)
                    .account(account)
                    .debit(req.debit)
                    .credit(req.credit)
                    .build();
            lines.add(line);
        }

        journal.setLines(lines);
        journalEntryRepository.save(journal);
    }

    // DTO for internal use
    public record EntryRequest(String accountCode, BigDecimal debit, BigDecimal credit) {}
}