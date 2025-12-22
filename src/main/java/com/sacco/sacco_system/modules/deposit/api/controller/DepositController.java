package com.sacco.sacco_system.modules.deposit.api.controller;

import com.sacco.sacco_system.modules.audit.domain.entity.AuditLog;
import com.sacco.sacco_system.modules.audit.domain.service.AuditService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.deposit.api.dto.CreateDepositRequest;
import com.sacco.sacco_system.modules.deposit.api.dto.DepositDTO;
import com.sacco.sacco_system.modules.deposit.api.dto.DepositProductDTO;
import com.sacco.sacco_system.modules.deposit.domain.service.DepositProductService;
import com.sacco.sacco_system.modules.deposit.domain.service.DepositService;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deposit Controller
 * Handles member deposit operations
 */
@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final DepositService depositService;
    private final DepositProductService depositProductService;
    private final MemberRepository memberRepository;
    private final AuditService auditService;

    /**
     * Create a new deposit with allocations
     * POST /api/deposits/create
     */
    @PostMapping("/create")
    public ResponseEntity<?> createDeposit(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateDepositRequest request) {
        
        try {
            // Get member
            Member member = memberRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            // Process deposit
            DepositDTO deposit = depositService.processDeposit(member, request);

            // Log successful deposit
            auditService.logSuccess(user, AuditLog.Actions.CREATE, "Deposit", 
                deposit.getId().toString(), 
                "Deposit of KES " + request.getTotalAmount() + " processed successfully");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Deposit processed successfully");
            response.put("deposit", deposit);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            auditService.logFailure(user, AuditLog.Actions.CREATE, "Deposit", null, 
                "Deposit failed: " + e.getMessage(), e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (SecurityException e) {
            auditService.logFailure(user, AuditLog.Actions.CREATE, "Deposit", null, 
                "Deposit access denied: " + e.getMessage(), e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (Exception e) {
            auditService.logFailure(user, AuditLog.Actions.CREATE, "Deposit", null, 
                "Deposit processing error: " + e.getMessage(), e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to process deposit: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get member's deposit history
     * GET /api/deposits/my-history
     */
    @GetMapping("/my-history")
    public ResponseEntity<?> getMyDeposits(@AuthenticationPrincipal User user) {
        try {
            Member member = memberRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            List<DepositDTO> deposits = depositService.getMemberDeposits(member);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deposits", deposits);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get deposit by ID
     * GET /api/deposits/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDeposit(@PathVariable UUID id) {
        try {
            DepositDTO deposit = depositService.getDepositById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deposit", deposit);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Get all available contribution products
     * GET /api/deposits/products/available
     */
    @GetMapping("/products/available")
    public ResponseEntity<?> getAvailableProducts() {
        try {
            List<DepositProductDTO> products = depositProductService.getActiveProducts();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
