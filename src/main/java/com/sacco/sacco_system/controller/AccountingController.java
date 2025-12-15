package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.entity.accounting.FiscalPeriod;
import com.sacco.sacco_system.entity.accounting.GLAccount;
import com.sacco.sacco_system.entity.accounting.GlMapping;
import com.sacco.sacco_system.entity.accounting.JournalEntry;
import com.sacco.sacco_system.repository.accounting.FiscalPeriodRepository;
import com.sacco.sacco_system.repository.accounting.GLAccountRepository;
import com.sacco.sacco_system.repository.accounting.GlMappingRepository;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final GLAccountRepository accountRepository;
    private final JournalEntryRepository journalRepository;
    private final AccountingService accountingService;

    // ✅ NEW REPOSITORIES
    private final GlMappingRepository glMappingRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    // --- 1. CORE ACCOUNTING ---

    @GetMapping("/accounts")
    public ResponseEntity<Map<String, Object>> getChartOfAccounts() {
        List<GLAccount> accounts = accountRepository.findAll(Sort.by("code"));
        return ResponseEntity.ok(Map.of("success", true, "data", accounts));
    }

    @GetMapping("/journal")
    public ResponseEntity<Map<String, Object>> getJournalEntries() {
        List<JournalEntry> entries = journalRepository.findAll(Sort.by(Sort.Direction.DESC, "transactionDate"));
        return ResponseEntity.ok(Map.of("success", true, "data", entries));
    }

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getAccountingReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        if (endDate == null) endDate = LocalDate.now();
        List<GLAccount> reportData = accountingService.getAccountsWithBalancesAsOf(startDate, endDate);
        return ResponseEntity.ok(Map.of("success", true, "data", reportData));
    }

    @PutMapping("/accounts/{code}/toggle")
    public ResponseEntity<Map<String, Object>> toggleAccountStatus(@PathVariable String code) {
        GLAccount updatedAccount = accountingService.toggleAccountStatus(code);
        return ResponseEntity.ok(Map.of("success", true, "message", "Account status updated", "data", updatedAccount));
    }

    @PostMapping("/accounts")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody GLAccount account) {
        GLAccount newAccount = accountingService.createManualAccount(account);
        return ResponseEntity.ok(Map.of("success", true, "message", "Account created successfully", "data", newAccount));
    }

    @PostMapping("/journal")
    public ResponseEntity<Map<String, Object>> postManualEntry(@RequestBody AccountingService.ManualEntryRequest request) {
        accountingService.postManualJournalEntry(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Journal Entry Posted Successfully"));
    }

    // --- 2. CONFIGURATION: GL MAPPINGS ---

    @GetMapping("/config/mappings")
    public ResponseEntity<Map<String, Object>> getGlMappings() {
        return ResponseEntity.ok(Map.of("success", true, "data", glMappingRepository.findAll()));
    }

    @PutMapping("/config/mappings")
    public ResponseEntity<Map<String, Object>> updateGlMapping(@RequestBody GlMapping mapping) {
        GlMapping saved = glMappingRepository.save(mapping);
        return ResponseEntity.ok(Map.of("success", true, "message", "Mapping Updated", "data", saved));
    }

    // --- 3. CONFIGURATION: FISCAL PERIODS ---

    @GetMapping("/config/periods")
    public ResponseEntity<Map<String, Object>> getFiscalPeriods() {
        return ResponseEntity.ok(Map.of("success", true, "data", fiscalPeriodRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate"))));
    }

    @PostMapping("/config/periods")
    public ResponseEntity<Map<String, Object>> createFiscalPeriod(@RequestBody FiscalPeriod period) {
        FiscalPeriod saved = fiscalPeriodRepository.save(period);
        return ResponseEntity.ok(Map.of("success", true, "message", "Period Created", "data", saved));
    }

    @PutMapping("/config/periods/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleFiscalPeriod(@PathVariable UUID id) {
        FiscalPeriod period = fiscalPeriodRepository.findById(id).orElseThrow();
        period.setActive(!period.isActive());
        if (period.isActive()) {
            // Deactivate others? Logic can be added here if needed to enforce single active period
        }
        fiscalPeriodRepository.save(period);
        return ResponseEntity.ok(Map.of("success", true, "message", "Status Changed"));
    }
}