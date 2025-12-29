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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final GLAccountRepository accountRepository;
    private final JournalEntryRepository journalRepository;
    private final AccountingService accountingService;
    private final GlMappingRepository glMappingRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    // --- 0. SETUP & INITIALIZATION ---

    @PostMapping("/setup/initialize")
    public ResponseEntity<Map<String, Object>> initializeAccounting() {
        try {
            if (accountRepository.count() > 0) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Chart of Accounts already initialized. Use /reset endpoint to reinitialize."
                ));
            }
            accountingService.initChartOfAccounts();
            accountingService.initDefaultMappings();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Chart of Accounts and GL Mappings initialized successfully",
                    "accountsCreated", accountRepository.count(),
                    "mappingsCreated", glMappingRepository.count()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error initializing accounting: " + e.getMessage()));
        }
    }

    @PostMapping("/setup/reset")
    public ResponseEntity<Map<String, Object>> resetAccounting() {
        try {
            glMappingRepository.deleteAll();
            accountRepository.deleteAll();
            accountingService.initChartOfAccounts();
            accountingService.initDefaultMappings();

            return ResponseEntity.ok(Map.of("success", true, "message", "Chart of Accounts reset and reinitialized successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error resetting accounting: " + e.getMessage()));
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

    // ✅ NEW ENDPOINT: Get only Active Asset Accounts (Potential Banks)
    @GetMapping("/accounts/active-banks")
    public ResponseEntity<Map<String, Object>> getActiveBankAccounts() {
        List<GLAccount> accounts = accountRepository.findAll(Sort.by("code"));
        
        // Filter for Active Assets. 
        // Note: In a robust system, you might have a 'SUB_TYPE' = 'BANK', but 'ASSET' is a safe default for now.
        List<GLAccount> activeBanks = accounts.stream()
            .filter(a -> a.isActive() && "ASSET".equals(a.getType().toString())) 
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of("success", true, "data", activeBanks));
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
        List<GLAccount> reportData = accountingService.getAccountsWithBalancesAsOf(startDate, endDate);
        return ResponseEntity.ok(Map.of("success", true, "data", reportData != null ? reportData : List.of()));
    }

    @GetMapping("/report/activity")
    public ResponseEntity<Map<String, Object>> getLedgerActivityReport(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        List<Map<String, Object>> activity = accountingService.getLedgerActivity(startDate, endDate);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("activity", activity)));
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
            return ResponseEntity.ok(Map.of("success", true, "message", "Account created successfully", "data", created));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to create account: " + e.getMessage()));
        }
    }

    @PostMapping("/journal")
    public ResponseEntity<Map<String, Object>> postManualEntry(@RequestBody AccountingService.ManualEntryRequest request) {
        accountingService.postManualJournalEntry(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Journal Entry Posted Successfully"));
    }

    // --- 2. CONFIGURATION: GL MAPPINGS (UPDATED) ---

    @GetMapping("/config/mappings")
    public ResponseEntity<Map<String, Object>> getGlMappings() {
        return ResponseEntity.ok(Map.of("success", true, "data", glMappingRepository.findAll()));
    }

    // ✅ ADDED: Create New Rule Endpoint
    @PostMapping("/config/mappings")
    public ResponseEntity<Map<String, Object>> createGlMapping(@RequestBody GlMapping mapping) {
        if(glMappingRepository.findByEventName(mapping.getEventName()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mapping for '" + mapping.getEventName() + "' already exists."));
        }
        GlMapping saved = glMappingRepository.save(mapping);
        return ResponseEntity.ok(Map.of("success", true, "message", "Mapping Created Successfully", "data", saved));
    }

    @PutMapping("/config/mappings")
    public ResponseEntity<Map<String, Object>> updateGlMapping(@RequestBody GlMapping mapping) {
        GlMapping saved = glMappingRepository.save(mapping);
        return ResponseEntity.ok(Map.of("success", true, "message", "Mapping Updated", "data", saved));
    }

    // ✅ ADDED: Delete Rule Endpoint
   @DeleteMapping("/config/mappings/{eventName}") // Change from {id}
public ResponseEntity<Map<String, Object>> deleteGlMapping(@PathVariable String eventName) { // Change UUID to String
    glMappingRepository.deleteById(eventName); // Fixes incompatible types error
    return ResponseEntity.ok(Map.of("success", true, "message", "Mapping Deleted"));
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
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Start date and end date are required"));
            }
            if (period.getStartDate().isAfter(period.getEndDate())) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Start date must be before end date"));
            }
            FiscalPeriod saved = fiscalPeriodRepository.save(period);
            return ResponseEntity.ok(Map.of("success", true, "message", "Period Created", "data", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to create fiscal period: " + e.getMessage()));
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