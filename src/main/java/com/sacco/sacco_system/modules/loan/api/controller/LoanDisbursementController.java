package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanDisbursement;
import com.sacco.sacco_system.modules.loan.domain.service.LoanDisbursementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loans/disbursements")
@RequiredArgsConstructor
public class LoanDisbursementController {

    private final LoanDisbursementService disbursementService;

    @GetMapping("/pending-preparation")
    public ResponseEntity<List<LoanDisbursement>> getPendingPreparation() {
        return ResponseEntity.ok(disbursementService.getDisbursementsAwaitingApproval());
    }

    @PostMapping("/prepare/{loanId}")
    public ResponseEntity<?> prepare(@PathVariable UUID loanId, @RequestBody Map<String, Object> request) {
        try {
            LoanDisbursement.DisbursementMethod method = LoanDisbursement.DisbursementMethod.valueOf((String) request.get("method"));
            Map<String, String> details = (Map<String, String>) request.get("details");
            String notes = (String) request.get("notes");
            return ResponseEntity.ok(disbursementService.prepareDisbursement(loanId, method, details, notes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/complete/{disbursementId}")
    public ResponseEntity<?> complete(@PathVariable UUID disbursementId, @RequestBody Map<String, String> request) {
        try {
            // This triggers the accounting postLoanDisbursement and sets loan status to DISBURSED
            return ResponseEntity.ok(disbursementService.completeDisbursement(
                disbursementId, 
                request.get("reference"), 
                request.get("notes"), 
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}