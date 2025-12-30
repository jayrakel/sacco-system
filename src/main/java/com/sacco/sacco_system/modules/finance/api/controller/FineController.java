package com.sacco.sacco_system.modules.finance.api.controller;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.finance.domain.entity.Fine;
import com.sacco.sacco_system.modules.finance.domain.service.FineService;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fine Controller
 * Manages fines and penalties for members
 */
@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FineController {

    private final FineService fineService;
    private final MemberRepository memberRepository;

    /**
     * Impose a fine on a member
     */
    @PostMapping("/impose")
    public ResponseEntity<Map<String, Object>> imposeFine(@RequestBody Map<String, Object> request) {
        try {
            UUID memberId = UUID.fromString((String) request.get("memberId"));

            // Handle optional loanId safely
            String loanIdStr = (String) request.get("loanId");
            UUID loanId = (loanIdStr != null && !loanIdStr.isEmpty()) ? UUID.fromString(loanIdStr) : null;

            Fine.FineType type = Fine.FineType.valueOf((String) request.get("type"));
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String description = (String) request.get("description");

            Integer daysOverdue = request.get("daysOverdue") != null ?
                    Integer.parseInt(request.get("daysOverdue").toString()) : null;

            Fine fine = fineService.imposeFine(memberId, loanId, type, amount, description, daysOverdue);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Fine imposed successfully",
                    "data", fine
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Pay a fine
     */
    @PostMapping("/pay/{fineId}")
    public ResponseEntity<Map<String, Object>> payFine(
            @PathVariable UUID fineId,
            @RequestBody Map<String, String> request) {
        try {
            String paymentReference = request.get("paymentReference");
            Fine fine = fineService.payFine(fineId, paymentReference);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Fine paid successfully",
                    "data", fine
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Waive a fine
     */
    @PostMapping("/waive/{fineId}")
    public ResponseEntity<Map<String, Object>> waiveFine(
            @PathVariable UUID fineId,
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            Fine fine = fineService.waiveFine(fineId, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Fine waived successfully",
                    "data", fine
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get member's fines
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<Map<String, Object>> getMemberFines(@PathVariable UUID memberId) {
        List<Fine> fines = fineService.getMemberFines(memberId);
        BigDecimal pendingTotal = fineService.getMemberPendingFinesTotal(memberId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "fines", fines,
                        "pendingTotal", pendingTotal
                )
        ));
    }

    /**
     * Get current user's fines (optionally filtered by status)
     */
    @GetMapping("/my-fines")
    public ResponseEntity<Map<String, Object>> getMyFines(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User user) {
        try {
            Member member = memberRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new RuntimeException("Member profile not found for this user"));

            List<Fine> fines;
            if ("PENDING".equalsIgnoreCase(status)) {
                fines = fineService.getMemberPendingFines(member.getId());
            } else {
                fines = fineService.getMemberFines(member.getId());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fines", fines);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get member's pending fines only
     */
    @GetMapping("/member/{memberId}/pending")
    public ResponseEntity<Map<String, Object>> getMemberPendingFines(@PathVariable UUID memberId) {
        List<Fine> fines = fineService.getMemberPendingFines(memberId);
        BigDecimal total = fineService.getMemberPendingFinesTotal(memberId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "fines", fines,
                        "total", total
                )
        ));
    }

    /**
     * Get all pending fines (admin)
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getAllPendingFines() {
        List<Fine> fines = fineService.getAllPendingFines();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", fines
        ));
    }

    /**
     * Get fine statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getFineStatistics() {
        Map<String, Object> stats = fineService.getFineStatistics();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
        ));
    }

    /**
     * Process overdue payments and impose fines (admin/automated)
     */
    @PostMapping("/process-overdue")
    public ResponseEntity<Map<String, Object>> processOverduePayments() {
        try {
            // ✅ FIX: Disabled temporarily for Skeleton Phase
            // List<Fine> fines = fineService.processOverduePayments();
            List<Fine> fines = Collections.emptyList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Automatic fine processing is disabled in Skeleton Mode.",
                    "data", fines
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}