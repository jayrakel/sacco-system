package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.core.dto.ApiResponse;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.service.LoanOfficerService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loan Officer endpoints for reviewing and approving loans
 * Requires LOAN_OFFICER or ADMIN role
 */
@RestController
@RequestMapping("/api/loan-officer")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
public class LoanOfficerController {

    private final LoanOfficerService loanOfficerService;
    private final LoanReadService loanReadService;

    /**
     * Get all submitted loans pending review
     */
    @GetMapping("/pending-loans")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingLoans() {
        List<Map<String, Object>> loans = loanReadService.getPendingLoansForOfficer();
        return ResponseEntity.ok(new ApiResponse<>(true, "Pending loans retrieved", loans));
    }

    /**
     * Get detailed loan information for review
     */
    @GetMapping("/loans/{loanId}")
    public ResponseEntity<ApiResponse<Loan>> getLoanDetails(@PathVariable UUID loanId) {
        Loan loan = loanOfficerService.getLoanForReview(loanId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Loan details retrieved", loan));
    }

    /**
     * Start reviewing a loan (moves to UNDER_REVIEW)
     */
    @PostMapping("/loans/{loanId}/start-review")
    public ResponseEntity<ApiResponse<Loan>> startReview(
            @PathVariable UUID loanId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Loan loan = loanOfficerService.startReview(loanId, userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(true, "Loan moved to under review", loan));
    }

    /**
     * Approve a loan
     */
    @PostMapping("/loans/{loanId}/approve")
    public ResponseEntity<ApiResponse<Loan>> approveLoan(
            @PathVariable UUID loanId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        BigDecimal approvedAmount = new BigDecimal(payload.get("approvedAmount").toString());
        String notes = payload.getOrDefault("notes", "").toString();

        Loan loan = loanOfficerService.approveLoan(loanId, userDetails.getUsername(), approvedAmount, notes);
        return ResponseEntity.ok(new ApiResponse<>(true, "Loan approved successfully", loan));
    }

    /**
     * Reject a loan
     */
    @PostMapping("/loans/{loanId}/reject")
    public ResponseEntity<ApiResponse<Loan>> rejectLoan(
            @PathVariable UUID loanId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        String reason = payload.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Rejection reason is required", null));
        }

        Loan loan = loanOfficerService.rejectLoan(loanId, userDetails.getUsername(), reason);
        return ResponseEntity.ok(new ApiResponse<>(true, "Loan rejected", loan));
    }

    /**
     * Request more information from applicant
     */
    @PostMapping("/loans/{loanId}/request-info")
    public ResponseEntity<ApiResponse<Loan>> requestMoreInfo(
            @PathVariable UUID loanId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        String information = payload.get("information");
        if (information == null || information.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Information request details are required", null));
        }

        Loan loan = loanOfficerService.requestMoreInformation(loanId, userDetails.getUsername(), information);
        return ResponseEntity.ok(new ApiResponse<>(true, "Information request sent", loan));
    }

    /**
     * Get loan statistics for officer dashboard
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOfficerStatistics() {
        Map<String, Object> stats = loanReadService.getLoanOfficerStatistics();
        return ResponseEntity.ok(new ApiResponse<>(true, "Statistics retrieved", stats));
    }
}

