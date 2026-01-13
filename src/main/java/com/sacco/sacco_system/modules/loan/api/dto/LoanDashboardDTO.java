package com.sacco.sacco_system.modules.loan.api.dto;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanApplicationDraft;
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

    // --- Data Lists ---
    // ✅ FIX: Changed type from 'Loan' to 'LoanResponseDTO' to match the Service mapper
    // This allows the frontend to receive the calculated fields (nextPaymentDate, arrears, etc.)
    private List<LoanResponseDTO> activeLoans;           // Status: ACTIVE, IN_ARREARS
    private List<LoanResponseDTO> pendingApplications;   // Status: SUBMITTED, APPROVED

    // ✅ NEW: Support for "Resume Application"
    private List<LoanResponseDTO> loansInProgress;       // Status: PENDING_GUARANTORS (Step 2)

    private LoanApplicationDraft currentDraft;// Status: PENDING_FEE, FEE_PAID (Step 1)

    // --- Summary Metrics ---
    private int activeLoansCount;
    private double totalOutstandingBalance;
}