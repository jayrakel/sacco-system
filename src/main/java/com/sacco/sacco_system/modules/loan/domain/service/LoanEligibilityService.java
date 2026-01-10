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
     * ✅ DOMAIN LOGIC: Calculate Max Loan Limit
     * Rule: Limit = Total Savings * Global Multiplier (from System Settings)
     * This applies to ALL members regardless of employment status.
     */
    public BigDecimal calculateMaxLoanLimit(Member member) {
        // 1. Get Total Savings (Share Capital + Deposits)
        BigDecimal totalSavings = savingsAccountRepository.getTotalSavings(member.getId());

        // 2. Fetch Global Multiplier (Default to 3 if not set)
        String multiplierStr = systemSettingService.getString("LOAN_LIMIT_MULTIPLIER", "3");
        BigDecimal multiplier = new BigDecimal(multiplierStr);

        // 3. Calculate Limit
        return totalSavings.multiply(multiplier);
    }

    public Map<String, Object> checkEligibility(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Member profile not found", 400));

        // --- Fetch Guardrail Settings ---
        BigDecimal minSavings = new BigDecimal(systemSettingService.getString("MIN_SAVINGS_FOR_LOAN", "5000"));
        int maxActiveLoans = Integer.parseInt(systemSettingService.getString("MAX_ACTIVE_LOANS", "1"));
        int minMembershipMonths = Integer.parseInt(systemSettingService.getString("MIN_MONTHS_MEMBERSHIP", "3"));

        // --- Fetch Member Data ---
        BigDecimal currentSavings = savingsAccountRepository.getTotalSavings(member.getId());
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
        if (activeLoans >= maxActiveLoans) {
            isEligible = false;
            reasons.add("Max active loans reached");
        }
        if (monthsMember < minMembershipMonths) {
            isEligible = false;
            reasons.add("Membership duration too short (" + monthsMember + "/" + minMembershipMonths + " months)");
        }

        // --- Calculate Limit ---
        BigDecimal maxLoanLimit = calculateMaxLoanLimit(member);

        Map<String, Object> response = new HashMap<>();
        response.put("eligible", isEligible);
        response.put("reasons", reasons);
        response.put("currentSavings", currentSavings);
        response.put("maxLoanLimit", maxLoanLimit); // Frontend will use this for the slider
        response.put("currency", "KES");

        return response;
    }
}