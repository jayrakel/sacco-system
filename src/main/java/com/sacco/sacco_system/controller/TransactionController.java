package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.entity.Transaction;
import com.sacco.sacco_system.service.SavingsService;
import com.sacco.sacco_system.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final SavingsService savingsService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", transactions);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transferFunds(@RequestParam String fromAccount,
                                           @RequestParam String toAccount,
                                           @RequestParam BigDecimal amount,
                                           @RequestParam String description) {
        savingsService.transferFunds(fromAccount, toAccount, amount, description);
        return ResponseEntity.ok(Map.of("success", true, "message", "Transfer successful"));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<?> reverseTransaction(@PathVariable String id, @RequestParam String reason) {
        transactionService.reverseTransaction(id, reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Transaction reversed"));
    }

    @PostMapping("/interest")
    public ResponseEntity<?> applyInterest(@RequestParam BigDecimal rate) {
        savingsService.applyMonthlyInterest(rate);
        return ResponseEntity.ok(Map.of("success", true, "message", "Interest applied to all accounts"));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadStatement() {
        InputStreamResource file = new InputStreamResource(transactionService.generateCsvStatement());
        String filename = "sacco_statement_" + System.currentTimeMillis() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/csv"))
                .body(file);
    }
}