package com.sacco.sacco_system.modules.analytics.domain.service;

import com.sacco.sacco_system.modules.analytics.domain.service.impl.LoanAnalyticsReader;
import com.sacco.sacco_system.modules.analytics.domain.service.impl.MemberAnalyticsReader;
import com.sacco.sacco_system.modules.analytics.domain.service.impl.SavingsAnalyticsReader;
import com.sacco.sacco_system.modules.analytics.domain.service.impl.TrendAnalyticsReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Analytics Service (Facade)
 * Aggregates insights from specialized reader components.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final MemberAnalyticsReader memberReader;
    private final LoanAnalyticsReader loanReader;
    private final SavingsAnalyticsReader savingsReader;
    private final TrendAnalyticsReader trendReader;

    public Map<String, Object> getMemberGrowthAnalytics() {
        return memberReader.getMemberGrowthAnalytics();
    }

    public Map<String, Object> getLoanPortfolioAnalytics() {
        return loanReader.getLoanPortfolioAnalytics();
    }

    public Map<String, Object> getSavingsAnalytics() {
        return savingsReader.getSavingsAnalytics();
    }

    public Map<String, Object> getPerformanceTrends(int months) {
        return trendReader.getPerformanceTrends(months);
    }

    public Map<String, Object> getTopPerformers(int limit) {
        return memberReader.getTopPerformers(limit);
    }

    public Map<String, Object> getFinancialHealthScore() {
        // Reuse the loan portfolio data to avoid double calculation
        Map<String, Object> loanStats = loanReader.getLoanPortfolioAnalytics();
        return loanReader.getFinancialHealthScore(loanStats);
    }

    /**
     * Aggregates all analytics into a single dashboard view
     */
    public Map<String, Object> getDashboardStatistics() {
        return Map.of(
                "memberGrowth", memberReader.getMemberGrowthAnalytics(),
                "loanPortfolio", loanReader.getLoanPortfolioAnalytics(),
                "savingsAnalytics", savingsReader.getSavingsAnalytics(),
                "recentTrends", trendReader.getPerformanceTrends(6),
                "topPerformers", memberReader.getTopPerformers(10)
        );
    }
}