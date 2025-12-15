package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.entity.accounting.GLAccount;
import com.sacco.sacco_system.entity.accounting.JournalEntry;
import com.sacco.sacco_system.repository.accounting.GLAccountRepository;
import com.sacco.sacco_system.repository.accounting.JournalEntryRepository;
import com.sacco.sacco_system.service.AccountingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final GLAccountRepository accountRepository;
    private final JournalEntryRepository journalRepository;
    private final AccountingService accountingService;

    @GetMapping("/accounts")
    public ResponseEntity<Map<String, Object>> getChartOfAccounts() {
        List<GLAccount> accounts = accountRepository.findAll(Sort.by("code"));
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", accounts);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/journal")
    public ResponseEntity<Map<String, Object>> getJournalEntries() {
        List<JournalEntry> entries = journalRepository.findAll(Sort.by(Sort.Direction.DESC, "transactionDate"));
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", entries);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getAccountingReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        if (endDate == null) endDate = LocalDate.now();
        List<GLAccount> reportData = accountingService.getAccountsWithBalancesAsOf(startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", reportData);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/accounts/{code}/toggle")
    public ResponseEntity<Map<String, Object>> toggleAccountStatus(@PathVariable String code) {
        GLAccount updatedAccount = accountingService.toggleAccountStatus(code);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Account status updated");
        response.put("data", updatedAccount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody GLAccount account) {
        GLAccount newAccount = accountingService.createManualAccount(account);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Account created successfully");
        response.put("data", newAccount);
        return ResponseEntity.ok(response);
    }

    // ✅ NEW: Endpoint for Manual Journal Entries (Expenses, Assets)
    @PostMapping("/journal")
    public ResponseEntity<Map<String, Object>> postManualEntry(@RequestBody AccountingService.ManualEntryRequest request) {
        accountingService.postManualJournalEntry(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Journal Entry Posted Successfully");
        return ResponseEntity.ok(response);
    }
}