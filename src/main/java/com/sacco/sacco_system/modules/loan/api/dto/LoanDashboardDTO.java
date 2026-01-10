package com.sacco.sacco_system.modules.loan.api.dto;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
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
    // Changed to 'Loan' entity to match the updated Service and allow full access to details (e.g. Products)
    private List<Loan> activeLoans;           // Status: ACTIVE, IN_ARREARS
    private List<Loan> pendingApplications;   // Status: SUBMITTED, APPROVED

    // ✅ NEW: Support for "Resume Application"
    private List<Loan> loansInProgress;       // Status: PENDING_GUARANTORS (Step 2)
    private LoanApplicationDraft currentDraft;// Status: PENDING_FEE, FEE_PAID (Step 1)

    // --- Summary Metrics ---
    private int activeLoansCount;
    private double totalOutstandingBalance;
}