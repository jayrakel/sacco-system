package com.sacco.sacco_system.modules.finance.api.controller;

import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.savings.domain.service.SavingsService;
import com.sacco.sacco_system.modules.finance.domain.service.TransactionService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    // ✅ UPDATED: Endpoint now accepts optional Date Params
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Transaction> transactions = transactionService.getAllTransactions(startDate, endDate);

        // Convert to simplified DTO
        List<TransactionDTO> dtos = transactions.stream().map(t -> TransactionDTO.builder()
                .id(t.getId())
                .transactionId(t.getTransactionId())
                .amount(t.getAmount())
                .type(t.getType().toString())
                .transactionDate(t.getTransactionDate())
                .referenceCode(t.getReferenceCode())
                .description(t.getDescription())
                .member(t.getMember() != null ? MemberDTO.builder()
                        .firstName(t.getMember().getFirstName())
                        .lastName(t.getMember().getLastName())
                        .build() : null)
                .build()
        ).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", dtos);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<?> reverseTransaction(@PathVariable String id, @RequestParam String reason) {
        transactionService.reverseTransaction(id, reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Transaction reversed"));
    }

    @PostMapping("/interest")
    public ResponseEntity<?> applyInterest() {
        savingsService.applyWeeklyInterest();
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

    @PostMapping("/record-payment")
    public ResponseEntity<Map<String, Object>> recordProcessingFeePayment(
            @RequestParam BigDecimal amount,
            @RequestParam String referenceCode,
            @RequestParam String type) {
        try {
            transactionService.recordProcessingFee(amount, referenceCode, type);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Processing fee payment recorded successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Data
    @Builder
    public static class TransactionDTO {
        private UUID id;
        private String transactionId;
        private BigDecimal amount;
        private String type;
        private LocalDateTime transactionDate;
        private String referenceCode;
        private String description;
        private MemberDTO member;
    }

    @Data
    @Builder
    public static class MemberDTO {
        private String firstName;
        private String lastName;
    }
}