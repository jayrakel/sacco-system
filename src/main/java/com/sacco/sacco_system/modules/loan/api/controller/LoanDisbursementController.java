package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanDisbursement;
import com.sacco.sacco_system.modules.loan.domain.service.LoanDisbursementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loans/disbursements")
@RequiredArgsConstructor
public class LoanDisbursementController {

    private final LoanDisbursementService disbursementService;

    // 1. Prepare Disbursement (Treasurer enters details)
    @PostMapping("/prepare")
    public ResponseEntity<?> prepareDisbursement(@RequestBody Map<String, Object> payload) {
        try {
            UUID loanId = UUID.fromString(payload.get("loanId").toString());
            String methodStr = payload.get("method").toString();
            String notes = payload.getOrDefault("notes", "").toString();
            Map<String, String> details = (Map<String, String>) payload.get("details");

            LoanDisbursement.DisbursementMethod method = LoanDisbursement.DisbursementMethod.valueOf(methodStr);

            return ResponseEntity.ok(Map.of("success", true,
                    "data", disbursementService.prepareDisbursement(loanId, method, details, notes)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 2. Complete Disbursement (Move Money & Activate Loan)
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeDisbursement(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        try {
            String ref = payload.get("transactionReference");
            String notes = payload.getOrDefault("notes", "");

            // ✅ FIX: Only pass 3 arguments (ID, Ref, Notes).
            // Do NOT pass a 4th argument (additionalDetails/null).
            return ResponseEntity.ok(Map.of("success", true,
                    "data", disbursementService.completeDisbursement(id, ref, notes)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- QUERIES ---

    @GetMapping("/pending")
    public ResponseEntity<?> getPending() {
        // Returns items that are Prepared but not yet Completed
        return ResponseEntity.ok(Map.of("success", true, "data", disbursementService.getPendingDisbursements()));
    }

    @GetMapping("/awaiting-approval")
    public ResponseEntity<?> getAwaitingApproval() {
        return ResponseEntity.ok(Map.of("success", true, "data", disbursementService.getDisbursementsAwaitingApproval()));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(Map.of("success", true, "data", disbursementService.getDisbursementStatistics()));
    }
}