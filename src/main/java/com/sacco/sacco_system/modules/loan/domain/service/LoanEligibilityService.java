package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanEligibilityService {

    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;
    private final SystemSettingService systemSettingService;

    /**
     * ✅ DOMAIN LOGIC: Calculate Gross Loan Limit
     * Rule: Limit = Total Savings * Global Multiplier (from System Settings)
     */
    public BigDecimal calculateMaxLoanLimit(Member member) {
        // 1. Get Total Savings (Share Capital + Deposits)
        BigDecimal totalSavings = savingsAccountRepository.getTotalSavings(member.getId());
        if (totalSavings == null) totalSavings = BigDecimal.ZERO;

        // 2. Fetch Global Multiplier (Default to 3 if not set)
        String multiplierStr = systemSettingService.getString("LOAN_LIMIT_MULTIPLIER", "3");
        BigDecimal multiplier = new BigDecimal(multiplierStr);

        // 3. Calculate Limit
        return totalSavings.multiply(multiplier);
    }

    /**
     * ✅ NEW: Detailed Limits for Dashboard & Guarantor Manager
     * Used by GET /api/loans/limits to populate the "Self-Guarantee" bar correctly.
     */
    public Map<String, Object> getLoanLimits(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member not found", 404));

        // 1. Total Savings (The Actual Money you have)
        // This is what the frontend needs to stop "guessing"
        BigDecimal totalDeposits = savingsAccountRepository.getTotalSavings(member.getId());
        if (totalDeposits == null) totalDeposits = BigDecimal.ZERO;

        // 2. Gross Limit (e.g. 3x Savings)
        BigDecimal grossLimit = calculateMaxLoanLimit(member);

        // 3. Current Liabilities (Active Loans that eat into your limit)
        BigDecimal currentLiability = loanRepository.getTotalOutstandingBalance(member.getId());
        if (currentLiability == null) currentLiability = BigDecimal.ZERO;

        // 4. Net Eligible (Gross - Liability)
        BigDecimal netEligible = grossLimit.subtract(currentLiability);
        if (netEligible.compareTo(BigDecimal.ZERO) < 0) netEligible = BigDecimal.ZERO;

        Map<String, Object> response = new HashMap<>();
        response.put("maxEligibleAmount", netEligible); // What you can borrow NOW
        response.put("grossLimit", grossLimit);         // Your theoretical max (3x Savings)
        response.put("totalDeposits", totalDeposits);   // ✅ The Fix: Your actual savings
        response.put("currentLiability", currentLiability);
        response.put("currency", "KES");

        return response;
    }

    /**
     * General Eligibility Check (Min Savings, Membership Duration, etc.)
     */
    public Map<String, Object> checkEligibility(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member profile not found", 400));

        // --- Fetch Guardrail Settings ---
        BigDecimal minSavings = new BigDecimal(systemSettingService.getString("MIN_SAVINGS_FOR_LOAN", "5000"));
        int maxActiveLoans = Integer.parseInt(systemSettingService.getString("MAX_ACTIVE_LOANS", "1"));
        int minMembershipMonths = Integer.parseInt(systemSettingService.getString("MIN_MONTHS_MEMBERSHIP", "3"));

        // --- Fetch Member Data ---
        BigDecimal currentSavings = savingsAccountRepository.getTotalSavings(member.getId());
        if (currentSavings == null) currentSavings = BigDecimal.ZERO;

        long activeLoans = loanRepository.countActiveLoans(member.getId());

        LocalDate joinDate = (member.getMembershipDate() != null)
                ? member.getMembershipDate().toLocalDate()
                : (member.getCreatedAt() != null ? member.getCreatedAt().toLocalDate() : LocalDate.now());
        long monthsMember = ChronoUnit.MONTHS.between(joinDate, LocalDate.now());

        // --- Eligibility Logic ---
        boolean isEligible = true;
        List<String> reasons = new ArrayList<>();

        if (currentSavings.compareTo(minSavings) < 0) {
            isEligible = false;
            reasons.add("Insufficient Savings (Min: " + minSavings + ")");
        }
        // Note: We might allow multiple loans if within limit, but keeping config check for now
        if (activeLoans >= maxActiveLoans) {
            isEligible = false;
            reasons.add("Max active loans reached");
        }
        if (monthsMember < minMembershipMonths) {
            isEligible = false;
            reasons.add("Membership duration too short (" + monthsMember + "/" + minMembershipMonths + " months)");
        }

        // --- Calculate Limit ---
        // We reuse the getLoanLimits logic to return consistent data
        Map<String, Object> limits = getLoanLimits(email);

        Map<String, Object> response = new HashMap<>();
        response.put("eligible", isEligible);
        response.put("reasons", reasons);
        response.put("currentSavings", currentSavings);
        response.put("maxLoanLimit", limits.get("maxEligibleAmount"));
        response.put("totalDeposits", limits.get("totalDeposits")); // Ensure consistency
        response.put("currency", "KES");

        return response;
    }
}