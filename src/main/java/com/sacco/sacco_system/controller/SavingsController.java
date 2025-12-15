package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.dto.SavingsAccountDTO;
import com.sacco.sacco_system.entity.SavingsAccount;
import com.sacco.sacco_system.entity.SavingsProduct;
import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.repository.MemberRepository;
import com.sacco.sacco_system.repository.SavingsAccountRepository;
import com.sacco.sacco_system.repository.SavingsProductRepository;
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
    private final SavingsProductRepository savingsProductRepository;
    private final SavingsAccountRepository savingsRepository;
    private final MemberRepository memberRepository;

    // ========================================================================
    // 1. SAVINGS PRODUCTS (Configuration)
    // ========================================================================

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        return ResponseEntity.ok(Map.of("success", true, "data", savingsProductRepository.findAll()));
    }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody SavingsProduct product) {
        try {
            SavingsProduct saved = savingsProductRepository.save(product);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "message", "Savings Product Created", "data", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 2. ACCOUNT MANAGEMENT (Opening & Retrieval)
    // ========================================================================

    @PostMapping("/open")
    public ResponseEntity<Map<String, Object>> openAccount(
            @RequestParam UUID memberId,
            @RequestParam UUID productId,
            @RequestParam(required = false, defaultValue = "0") BigDecimal initialDeposit) {
        try {
            SavingsAccountDTO account = savingsService.openAccount(memberId, productId, initialDeposit);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", account);
            response.put("message", "Savings account opened successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/account/{id}")
    public ResponseEntity<Map<String, Object>> getSavingsAccountById(@PathVariable UUID id) {
        try {
            SavingsAccountDTO account = savingsService.getSavingsAccountById(id);
            return ResponseEntity.ok(Map.of("success", true, "data", account));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/account/number/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getSavingsAccountByNumber(@PathVariable String accountNumber) {
        try {
            SavingsAccountDTO account = savingsService.getSavingsAccountByNumber(accountNumber);
            return ResponseEntity.ok(Map.of("success", true, "data", account));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<Map<String, Object>> getSavingsAccountsByMemberId(@PathVariable UUID memberId) {
        List<SavingsAccountDTO> accounts = savingsService.getSavingsAccountsByMemberId(memberId);
        return ResponseEntity.ok(Map.of("success", true, "data", accounts, "count", accounts.size()));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSavingsAccounts() {
        List<SavingsAccountDTO> accounts = savingsService.getAllSavingsAccounts();
        return ResponseEntity.ok(Map.of("success", true, "data", accounts, "count", accounts.size()));
    }

    @GetMapping("/total-balance")
    public ResponseEntity<Map<String, Object>> getTotalBalance() {
        BigDecimal totalBalance = savingsService.getTotalSavingsBalance();
        return ResponseEntity.ok(Map.of("success", true, "totalBalance", totalBalance));
    }

    // ========================================================================
    // 3. TRANSACTIONS (Deposit & Withdraw)
    // ========================================================================

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        try {
            SavingsAccountDTO account = savingsService.deposit(accountNumber, amount, description);
            return ResponseEntity.ok(Map.of("success", true, "message", "Deposit successful", "data", account));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        try {
            SavingsAccountDTO account = savingsService.withdraw(accountNumber, amount, description);
            return ResponseEntity.ok(Map.of("success", true, "message", "Withdrawal successful", "data", account));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 4. MEMBER DASHBOARD (Self-Service)
    // ========================================================================

    @GetMapping("/my-balance")
    public ResponseEntity<Map<String, Object>> getMyBalance() {
        try {
            // 1. Get Logged-in User
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!(principal instanceof User)) {
                throw new RuntimeException("User not authenticated");
            }
            User user = (User) principal;

            // 2. Find Member Profile
            var member = memberRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new RuntimeException("Member profile not found for this user"));

            // 3. Get Accounts
            List<SavingsAccount> accounts = savingsRepository.findByMemberId(member.getId());
            if (accounts.isEmpty()) {
                throw new RuntimeException("No savings accounts found.");
            }

            // 4. Return Summary (Summing all balances for simple view, or returning main account)
            BigDecimal totalBalance = accounts.stream()
                    .map(SavingsAccount::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accountNumber", accounts.get(0).getAccountNumber()); // Primary Account
            response.put("balance", totalBalance); // Total Balance across all products
            response.put("accounts", accounts); // List of all accounts

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}