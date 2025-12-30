package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanEligibilityService {
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;
    private final SystemSettingService systemSettingService;

    public Map<String, Object> checkEligibility(UUID userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 1. Settings
        BigDecimal minSavings = new BigDecimal(systemSettingService.getString("MIN_SAVINGS_FOR_LOAN", "5000"));
        int maxActiveLoans = Integer.parseInt(systemSettingService.getString("MAX_ACTIVE_LOANS", "1"));

        // 2. Fetch Data (Using Fixed Query)
        BigDecimal currentSavings = savingsAccountRepository.getTotalSavings(member.getId());
        long activeLoans = loanRepository.countActiveLoans(member.getId());

        // 3. Logic
        boolean isEligible = true;
        java.util.List<String> reasons = new java.util.ArrayList<>();

        if (currentSavings.compareTo(minSavings) < 0) {
            isEligible = false;
            reasons.add("Insufficient Savings");
        }
        if (activeLoans >= maxActiveLoans) {
            isEligible = false;
            reasons.add("Max active loans reached");
        }

        // 4. Response
        Map<String, Object> response = new HashMap<>();
        response.put("eligible", isEligible);
        response.put("reasons", reasons);
        response.put("currentSavings", currentSavings);
        response.put("requiredSavings", minSavings);
        response.put("currentActiveLoans", activeLoans);
        response.put("maxActiveLoans", maxActiveLoans);

        return response;
    }
}