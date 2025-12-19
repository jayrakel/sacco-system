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

    // âœ… NEW REPOSITORIES
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
        // TODO: Service returns List<Map<String,Object>> not List<GLAccount> - return empty list for now
        List<Map<String, Object>> reportData = accountingService.getAccountsWithBalancesAsOf(startDate, endDate);
        return ResponseEntity.ok(Map.of("success", true, "data", reportData != null ? reportData : List.of()));
    }

    @PutMapping("/accounts/{code}/toggle")
    public ResponseEntity<Map<String, Object>> toggleAccountStatus(@PathVariable String code) {
        // TODO: Service method returns void, stubbing with success response
        accountingService.toggleAccountStatus(code);
        return ResponseEntity.ok(Map.of("success", true, "message", "Account status updated"));
    }

    @PostMapping("/accounts")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody GLAccount account) {
        // TODO: Service method returns void, stubbing with success response
        accountingService.createManualAccount(account);
        return ResponseEntity.ok(Map.of("success", true, "message", "Account created successfully"));
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





