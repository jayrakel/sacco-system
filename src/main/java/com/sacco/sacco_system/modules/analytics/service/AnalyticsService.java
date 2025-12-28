package com.sacco.sacco_system.modules.analytics.service;

import com.sacco.sacco_system.modules.finance.domain.repository.ShareCapitalRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service
 * Provides advanced analytics and insights for the SACCO
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final ShareCapitalRepository shareCapitalRepository;

    /**
     * Get member growth analytics
     */
    public Map<String, Object> getMemberGrowthAnalytics() {
        List<Member> allMembers = memberRepository.findAll();

        // Group by month
        Map<String, Long> membersByMonth = allMembers.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getCreatedAt().toLocalDate().withDayOfMonth(1).toString(),
                        Collectors.counting()
                ));

        // Calculate growth rate
        long totalMembers = allMembers.size();
        long activeMembers = allMembers.stream()
                .filter(m -> m.getStatus() == Member.MemberStatus.ACTIVE)
                .count();

        return Map.of(
                "totalMembers", totalMembers,
                "activeMembers", activeMembers,
                "inactiveMembers", totalMembers - activeMembers,
                "membersByMonth", membersByMonth,
                "activePercentage", totalMembers > 0 ?
                        (activeMembers * 100.0 / totalMembers) : 0
        );
    }

    /**
     * Loan portfolio analytics removed with loans module — return neutral values.
     */
    public Map<String, Object> getLoanPortfolioAnalytics() {
        return Map.of(
                "totalLoans", 0,
                "activeLoans", 0,
                "completedLoans", 0,
                "defaultedLoans", 0,
                "totalDisbursed", BigDecimal.ZERO,
                "totalOutstanding", BigDecimal.ZERO,
                "totalRepaid", BigDecimal.ZERO,
                "portfolioAtRisk", BigDecimal.ZERO,
                "parPercentage", 0.0,
                "repaymentRate", 0.0
        );
    }

    /**
     * Get savings analytics
     */
    public Map<String, Object> getSavingsAnalytics() {
        BigDecimal totalSavings = savingsAccountRepository.getTotalActiveAccountsBalance();
        BigDecimal totalShareCapital = shareCapitalRepository.getTotalShareCapital();

        long totalAccounts = savingsAccountRepository.count();
        long activeAccounts = savingsAccountRepository.findAll().stream()
                .filter(a -> a.getStatus() == com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount.AccountStatus.ACTIVE)
                .count();

        // Get savings distribution
        List<BigDecimal> balances = savingsAccountRepository.findAll().stream()
                .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
                .sorted()
                .collect(Collectors.toList());

        BigDecimal averageSavings = balances.isEmpty() ? BigDecimal.ZERO :
                balances.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(balances.size()), 2, RoundingMode.HALF_UP);

        BigDecimal medianSavings = balances.isEmpty() ? BigDecimal.ZERO :
                balances.get(balances.size() / 2);

        return Map.of(
                "totalSavings", totalSavings != null ? totalSavings : BigDecimal.ZERO,
                "totalShareCapital", totalShareCapital != null ? totalShareCapital : BigDecimal.ZERO,
                "totalAccounts", totalAccounts,
                "activeAccounts", activeAccounts,
                "averageSavings", averageSavings,
                "medianSavings", medianSavings,
                "totalCapital", (totalSavings != null ? totalSavings : BigDecimal.ZERO)
                        .add(totalShareCapital != null ? totalShareCapital : BigDecimal.ZERO)
        );
    }

    /**
     * Get performance trends
     */
    public Map<String, Object> getPerformanceTrends(int months) {
        LocalDate startDate = LocalDate.now().minusMonths(months);

        List<Map<String, Object>> trends = new ArrayList<>();

        for (int i = 0; i < months; i++) {
            LocalDate monthStart = startDate.plusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            // Members joined this month
            long membersJoined = memberRepository.findAll().stream()
                    .filter(m -> {
                        LocalDate createdDate = m.getCreatedAt().toLocalDate();
                        return !createdDate.isBefore(monthStart) && !createdDate.isAfter(monthEnd);
                    })
                    .count();

            // Loans disbursed this month
            trends.add(Map.of(
                    "month", monthStart.toString(),
                    "membersJoined", membersJoined,
                    "loansDisbursed", 0
            ));
        }

        return Map.of(
                "period", months + " months",
                "trends", trends
        );
    }

    /**
     * Get top performers (members with highest savings)
     */
    public Map<String, Object> getTopPerformers(int limit) {
        List<Map<String, Object>> topSavers = memberRepository.findAll().stream()
                .sorted((m1, m2) -> {
                    BigDecimal s1 = m1.getTotalSavings() != null ? m1.getTotalSavings() : BigDecimal.ZERO;
                    BigDecimal s2 = m2.getTotalSavings() != null ? m2.getTotalSavings() : BigDecimal.ZERO;
                    return s2.compareTo(s1);
                })
                .limit(limit)
                .map(m -> {
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("memberNumber", m.getMemberNumber());
                    memberData.put("name", m.getFirstName() + " " + m.getLastName());
                    memberData.put("totalSavings", m.getTotalSavings() != null ? m.getTotalSavings() : BigDecimal.ZERO);
                    memberData.put("totalShares", m.getTotalShares() != null ? m.getTotalShares() : BigDecimal.ZERO);
                    return memberData;
                })
                .collect(Collectors.toList());

        return Map.of(
                "topSavers", topSavers
        );
    }

    /**
     * Get comprehensive dashboard statistics
     */
    public Map<String, Object> getDashboardStatistics() {
        return Map.of(
                "memberGrowth", getMemberGrowthAnalytics(),
                "loanPortfolio", getLoanPortfolioAnalytics(),
                "savingsAnalytics", getSavingsAnalytics(),
                "recentTrends", getPerformanceTrends(6),
                "topPerformers", getTopPerformers(10)
        );
    }

    /**
     * Get financial health score
     */
    public Map<String, Object> getFinancialHealthScore() {
        Map<String, Object> loanAnalytics = getLoanPortfolioAnalytics();
        Map<String, Object> savingsAnalytics = getSavingsAnalytics();

        // Calculate health score (0-100)
        double parPercentage = (double) loanAnalytics.get("parPercentage");
        double repaymentRate = (double) loanAnalytics.get("repaymentRate");

        // Lower PAR is better, higher repayment rate is better
        double healthScore = ((100 - parPercentage) * 0.5) + (repaymentRate * 0.5);

        String rating;
        if (healthScore >= 90) rating = "Excellent";
        else if (healthScore >= 75) rating = "Good";
        else if (healthScore >= 60) rating = "Fair";
        else if (healthScore >= 40) rating = "Poor";
        else rating = "Critical";

        return Map.of(
                "healthScore", healthScore,
                "rating", rating,
                "parPercentage", parPercentage,
                "repaymentRate", repaymentRate,
                "recommendations", getRecommendations(healthScore, parPercentage, repaymentRate)
        );
    }

    private List<String> getRecommendations(double healthScore, double parPercentage, double repaymentRate) {
        List<String> recommendations = new ArrayList<>();

        if (parPercentage > 5) {
            recommendations.add("Portfolio at Risk is high. Implement stricter loan approval criteria.");
        }
        if (repaymentRate < 80) {
            recommendations.add("Repayment rate is low. Consider introducing late payment penalties.");
        }
        if (healthScore < 60) {
            recommendations.add("Financial health needs attention. Review loan policies and collection procedures.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Financial health is strong. Continue current strategies.");
        }

        return recommendations;
    }
}

