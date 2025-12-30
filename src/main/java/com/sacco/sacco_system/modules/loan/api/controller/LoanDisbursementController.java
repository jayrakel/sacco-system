package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanDisbursement;
import com.sacco.sacco_system.modules.loan.domain.service.LoanDisbursementService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/loans") // ✅ FIX: Base path aligned with Frontend
@RequiredArgsConstructor
public class LoanDisbursementController {

    private final LoanDisbursementService disbursementService;
    private final LoanReadService readService;

    // --- 1. ONE-CLICK DISBURSEMENT (For Finance Dashboard) ---
    @PostMapping("/admin/{loanId}/disburse")
    public ResponseEntity<?> disburseLoan(@PathVariable UUID loanId) {
        try {
            disbursementService.processDisbursement(loanId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan disbursed successfully"));
        } catch (Exception e) {
            log.error("Disbursement failed", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- 2. DETAILED WORKFLOW (For Modal) ---

    @PostMapping("/disbursements/prepare")
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

    @PostMapping("/disbursements/{id}/complete")
    public ResponseEntity<?> completeDisbursement(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        try {
            String ref = payload.get("transactionReference");
            String notes = payload.getOrDefault("notes", "");

            // ✅ FIX: Pass empty map as 4th argument to match Service Signature
            return ResponseEntity.ok(Map.of("success", true,
                    "data", disbursementService.completeDisbursement(id, ref, notes, Collections.emptyMap())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- QUERIES ---

    @GetMapping("/disbursement/queue")
    public ResponseEntity<?> getDisbursementQueue() {
        // This calls the missing method in LoanReadService
        return ResponseEntity.ok(Map.of("success", true, "data", readService.getLoansReadyForDisbursement()));
    }

    @GetMapping("/disbursements/pending")
    public ResponseEntity<?> getPending() {
        return ResponseEntity.ok(Map.of("success", true, "data", disbursementService.getPendingDisbursements()));
    }

    @GetMapping("/disbursements/awaiting-approval")
    public ResponseEntity<?> getAwaitingApproval() {
        return ResponseEntity.ok(Map.of("success", true, "data", disbursementService.getDisbursementsAwaitingApproval()));
    }

    @GetMapping("/disbursements/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(Map.of("success", true, "data", disbursementService.getDisbursementStatistics()));
    }
}