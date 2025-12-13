package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.dto.SavingsAccountDTO;
import com.sacco.sacco_system.service.SavingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/savings")
@RequiredArgsConstructor
public class SavingsController {
    
    private final SavingsService savingsService;
    
    @PostMapping("/account")
    public ResponseEntity<Map<String, Object>> createSavingsAccount(@RequestParam Long memberId) {
        try {
            SavingsAccountDTO account = savingsService.createSavingsAccount(memberId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", account);
            response.put("message", "Savings account created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/account/{id}")
    public ResponseEntity<Map<String, Object>> getSavingsAccountById(@PathVariable Long id) {
        try {
            SavingsAccountDTO account = savingsService.getSavingsAccountById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", account);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    @GetMapping("/account/number/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getSavingsAccountByNumber(@PathVariable String accountNumber) {
        try {
            SavingsAccountDTO account = savingsService.getSavingsAccountByNumber(accountNumber);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", account);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    @GetMapping("/member/{memberId}")
    public ResponseEntity<Map<String, Object>> getSavingsAccountsByMemberId(@PathVariable Long memberId) {
        List<SavingsAccountDTO> accounts = savingsService.getSavingsAccountsByMemberId(memberId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", accounts);
        response.put("count", accounts.size());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSavingsAccounts() {
        List<SavingsAccountDTO> accounts = savingsService.getAllSavingsAccounts();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", accounts);
        response.put("count", accounts.size());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        try {
            SavingsAccountDTO account = savingsService.deposit(accountNumber, amount, description);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", account);
            response.put("message", "Deposit successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        try {
            SavingsAccountDTO account = savingsService.withdraw(accountNumber, amount, description);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", account);
            response.put("message", "Withdrawal successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/total-balance")
    public ResponseEntity<Map<String, Object>> getTotalBalance() {
        BigDecimal totalBalance = savingsService.getTotalSavingsBalance();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("totalBalance", totalBalance);
        return ResponseEntity.ok(response);
    }
}
