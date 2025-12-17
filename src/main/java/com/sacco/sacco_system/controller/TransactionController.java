package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.entity.Transaction;
import com.sacco.sacco_system.service.SavingsService;
import com.sacco.sacco_system.service.TransactionService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final SavingsService savingsService;

    // ✅ FIXED: Return a DTO instead of the raw Entity to prevent Infinite Recursion
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();

        // Convert to simplified DTO
        List<TransactionDTO> dtos = transactions.stream().map(t -> TransactionDTO.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .type(t.getType().toString())
                .transactionDate(t.getTransactionDate())
                .referenceCode(t.getReferenceCode())
                .description(t.getDescription())
                // Safely get member name
                .memberName(t.getMember() != null ? t.getMember().getFirstName() + " " + t.getMember().getLastName() : "System")
                .build()
        ).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", dtos);
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
    public ResponseEntity<?> applyInterest() {
        savingsService.applyMonthlyInterest();
        return ResponseEntity.ok(Map.of("success", true, "message", "Monthly Interest applied based on Product Rates"));
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

    // ✅ Simple Inner DTO Class
    @Data
    @Builder
    public static class TransactionDTO {
        private UUID id;
        private BigDecimal amount;
        private String type;
        private LocalDateTime transactionDate;
        private String referenceCode;
        private String description;
        private String memberName;
    }
}