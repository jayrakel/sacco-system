package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.entity.accounting.GLAccount;
import com.sacco.sacco_system.entity.accounting.JournalEntry;
import com.sacco.sacco_system.repository.accounting.GLAccountRepository;
import com.sacco.sacco_system.repository.accounting.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final GLAccountRepository accountRepository;
    private final JournalEntryRepository journalRepository;

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
}