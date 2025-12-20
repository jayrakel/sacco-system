package com.sacco.sacco_system.modules.finance.api.controller;

import com.sacco.sacco_system.modules.finance.domain.entity.Fine;
import com.sacco.sacco_system.modules.finance.domain.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    /**
     * Impose a fine on a member
     */
    @PostMapping("/impose")
    public ResponseEntity<Map<String, Object>> imposeFine(@RequestBody Map<String, Object> request) {
        try {
            UUID memberId = UUID.fromString((String) request.get("memberId"));
            UUID loanId = request.get("loanId") != null ?
                    UUID.fromString((String) request.get("loanId")) : null;
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
            List<Fine> fines = fineService.processOverduePayments();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Processed overdue payments. Imposed " + fines.size() + " fines.",
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

