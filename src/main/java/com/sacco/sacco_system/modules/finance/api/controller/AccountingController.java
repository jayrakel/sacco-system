package com.sacco.sacco_system.modules.finance.api.controller;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.FiscalPeriod;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GLAccount;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GlMapping;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.JournalEntry;
import com.sacco.sacco_system.modules.finance.domain.repository.FiscalPeriodRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.GLAccountRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.GlMappingRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.JournalEntryRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    
    // REMOVED: ChartOfAccountsSetupService dependency

    private final GlMappingRepository glMappingRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    // --- 0. SETUP & INITIALIZATION ---

    @PostMapping("/setup/initialize")
    public ResponseEntity<Map<String, Object>> initializeAccounting() {
        try {
            // Check directly using the repository instead of the deleted service
            if (accountRepository.count() > 0) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Chart of Accounts already initialized. Use /reset endpoint to reinitialize."
                ));
            }

            // Use AccountingService which loads from accounts.json
            accountingService.initChartOfAccounts();
            accountingService.initDefaultMappings();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Chart of Accounts and GL Mappings initialized successfully",
                    "accountsCreated", accountRepository.count(),
                    "mappingsCreated", glMappingRepository.count()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error initializing accounting: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/setup/reset")
    public ResponseEntity<Map<String, Object>> resetAccounting() {
        try {
            // Perform reset logic directly using repositories
            glMappingRepository.deleteAll();
            accountRepository.deleteAll();
            
            // Re-initialize using AccountingService
            accountingService.initChartOfAccounts();
            accountingService.initDefaultMappings();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Chart of Accounts reset and reinitialized successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error resetting accounting: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/setup/status")
    public ResponseEntity<Map<String, Object>> getSetupStatus() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "initialized", accountRepository.count() > 0,
                "accountsCount", accountRepository.count(),
                "mappingsCount", glMappingRepository.count(),
                "journalEntriesCount", journalRepository.count()
        ));
    }

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
        // Service returns List<GLAccount> with balances
        List<GLAccount> reportData = accountingService.getAccountsWithBalancesAsOf(startDate, endDate);
        return ResponseEntity.ok(Map.of("success", true, "data", reportData != null ? reportData : List.of()));
    }

    @PutMapping("/accounts/{code}/toggle")
    public ResponseEntity<Map<String, Object>> toggleAccountStatus(@PathVariable String code) {
        accountingService.toggleAccountStatus(code);
        return ResponseEntity.ok(Map.of("success", true, "message", "Account status updated"));
    }

    @PostMapping("/accounts")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody GLAccount account) {
        try {
            GLAccount created = accountingService.createManualAccount(account);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Account created successfully",
                    "data", created
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to create account: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            ));
        }
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
        try {
            if (period.getStartDate() == null || period.getEndDate() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Start date and end date are required"
                ));
            }
            if (period.getStartDate().isAfter(period.getEndDate())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Start date must be before end date"
                ));
            }
            FiscalPeriod saved = fiscalPeriodRepository.save(period);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Period Created",
                    "data", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to create fiscal period: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            ));
        }
    }

    @PutMapping("/config/periods/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleFiscalPeriod(@PathVariable UUID id) {
        FiscalPeriod period = fiscalPeriodRepository.findById(id).orElseThrow();
        period.setActive(!period.isActive());
        fiscalPeriodRepository.save(period);
        return ResponseEntity.ok(Map.of("success", true, "message", "Status Changed"));
    }
}