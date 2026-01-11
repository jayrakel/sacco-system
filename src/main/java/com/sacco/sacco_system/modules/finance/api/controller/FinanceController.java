package com.sacco.sacco_system.modules.finance.api.controller;

import com.sacco.sacco_system.modules.core.dto.ApiResponse;
import com.sacco.sacco_system.modules.finance.domain.service.DisbursementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FinanceController {

    private final DisbursementService disbursementService;

    /**
     * Get loans pending disbursement (APPROVED_BY_COMMITTEE status)
     */
    @GetMapping("/loans/pending-disbursement")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingDisbursements() {
        List<Map<String, Object>> loans = disbursementService.getLoansAwaitingDisbursement();
        return ResponseEntity.ok(new ApiResponse<>(true, "Pending disbursements retrieved", loans));
    }

    /**
     * Get disbursed loans
     */
    @GetMapping("/loans/disbursed")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDisbursedLoans() {
        List<Map<String, Object>> loans = disbursementService.getDisbursedLoans();
        return ResponseEntity.ok(new ApiResponse<>(true, "Disbursed loans retrieved", loans));
    }

    /**
     * Get finance statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        Map<String, Object> stats = disbursementService.getFinanceStatistics();
        return ResponseEntity.ok(new ApiResponse<>(true, "Statistics retrieved", stats));
    }

    /**
     * Disburse a loan
     */
    @PostMapping("/loans/{loanId}/disburse")
    public ResponseEntity<ApiResponse<Object>> disburseLoan(
            @PathVariable UUID loanId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        String disbursementMethod = payload.get("disbursementMethod");
        String phoneNumber = payload.get("phoneNumber");
        String reference = payload.get("reference");

        disbursementService.disburseLoan(loanId, disbursementMethod, phoneNumber, reference, userDetails.getUsername());

        return ResponseEntity.ok(new ApiResponse<>(true, "Loan disbursed successfully"));
    }

    /**
     * ✅ MIGRATION ENDPOINT: Recalculate outstanding amounts for existing loans
     * This fixes loans disbursed before calculation logic was added
     */
    @PostMapping("/loans/recalculate-outstanding")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recalculateOutstandingAmounts() {
        log.info("🔧 Starting loan recalculation...");
        Map<String, Object> result = disbursementService.recalculateExistingLoans();
        log.info("✅ Loan recalculation complete: {}", result);
        return ResponseEntity.ok(new ApiResponse<>(true, "Recalculation complete", result));
    }
}

