package com.sacco.sacco_system.modules.loan.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class LoanDashboardDTO {
    // --- UI State (The "Apply" Button) ---
    private boolean canApply;
    private String eligibilityMessage;
    private Map<String, Object> eligibilityDetails; // Detailed reasons/stats

    // --- Data List (The "Active Loans" Table) ---
    private List<LoanResponseDTO> activeLoans;

    // --- Summary Metrics ---
    private int activeLoansCount;
    private double totalOutstandingBalance;
}