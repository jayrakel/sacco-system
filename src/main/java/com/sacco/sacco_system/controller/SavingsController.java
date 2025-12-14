package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.dto.SavingsAccountDTO;
import com.sacco.sacco_system.entity.SavingsAccount;
import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.repository.MemberRepository;
import com.sacco.sacco_system.repository.SavingsAccountRepository;
import com.sacco.sacco_system.service.SavingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/savings")
@RequiredArgsConstructor
public class SavingsController {

    private final SavingsService savingsService;

    // ✅ Additional dependencies for the Dashboard Endpoint
    private final SavingsAccountRepository savingsRepository;
    private final MemberRepository memberRepository;

    @PostMapping("/account")
    public ResponseEntity<Map<String, Object>> createSavingsAccount(@RequestParam UUID memberId) {
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
    public ResponseEntity<Map<String, Object>> getSavingsAccountById(@PathVariable UUID id) {
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
    public ResponseEntity<Map<String, Object>> getSavingsAccountsByMemberId(@PathVariable UUID memberId) {
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

    // ✅ NEW ENDPOINT: For Member Dashboard
    @GetMapping("/my-balance")
    public ResponseEntity<Map<String, Object>> getMyBalance() {
        // 1. Get Logged-in User
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Find Member Profile linked to this User
        var member = memberRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Member profile not found for this user"));

        // 3. Get Active Savings Account
        SavingsAccount account = savingsRepository.findActiveAccountByMemberId(member.getId())
                .orElseThrow(() -> new RuntimeException("No active savings account found. Please contact admin."));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("accountNumber", account.getAccountNumber());
        response.put("balance", account.getBalance());

        return ResponseEntity.ok(response);
    }
}