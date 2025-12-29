package com.sacco.sacco_system.modules.analytics.domain.service.impl;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanAnalyticsReader {

    private final LoanRepository loanRepository;

    public Map<String, Object> getLoanPortfolioAnalytics() {
        List<Loan> allLoans = loanRepository.findAll();

        long totalLoans = allLoans.size();
        long activeLoans = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.DISBURSED).count();
        long completedLoans = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.COMPLETED).count();
        long defaultedLoans = allLoans.stream().filter(l -> l.getStatus() == Loan.LoanStatus.DEFAULTED).count();

        BigDecimal totalDisbursed = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.DISBURSED || l.getStatus() == Loan.LoanStatus.COMPLETED)
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.DISBURSED)
                .map(l -> l.getLoanBalance() != null ? l.getLoanBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRepaid = totalDisbursed.subtract(totalOutstanding);

        // Portfolio at Risk (PAR)
        BigDecimal portfolioAtRisk = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.DEFAULTED)
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double parPercentage = totalDisbursed.compareTo(BigDecimal.ZERO) > 0 ?
                portfolioAtRisk.divide(totalDisbursed, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;

        double repaymentRate = totalDisbursed.compareTo(BigDecimal.ZERO) > 0 ?
                totalRepaid.divide(totalDisbursed, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;

        return Map.of(
                "totalLoans", totalLoans,
                "activeLoans", activeLoans,
                "completedLoans", completedLoans,
                "defaultedLoans", defaultedLoans,
                "totalDisbursed", totalDisbursed,
                "totalOutstanding", totalOutstanding,
                "totalRepaid", totalRepaid,
                "portfolioAtRisk", portfolioAtRisk,
                "parPercentage", parPercentage,
                "repaymentRate", repaymentRate
        );
    }

    public Map<String, Object> getFinancialHealthScore(Map<String, Object> loanStats) {
        double parPercentage = (double) loanStats.get("parPercentage");
        double repaymentRate = (double) loanStats.get("repaymentRate");

        // Simple Health Score Algo
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
        if (parPercentage > 5) recommendations.add("Portfolio at Risk is high. Implement stricter loan approval criteria.");
        if (repaymentRate < 80) recommendations.add("Repayment rate is low. Consider introducing late payment penalties.");
        if (healthScore < 60) recommendations.add("Financial health needs attention. Review loan policies and collection procedures.");
        if (recommendations.isEmpty()) recommendations.add("Financial health is strong. Continue current strategies.");
        return recommendations;
    }
}