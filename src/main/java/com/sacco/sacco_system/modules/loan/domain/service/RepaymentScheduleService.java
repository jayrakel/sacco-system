package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepaymentScheduleService {

    /**
     * Calculate weekly repayment amount
     * Formula: (Principal + Interest) / Number of Weeks
     *
     * Interest = Principal × Interest Rate
     * Total Repayment = Principal + Interest
     * Weekly Installment = Total Repayment / Weeks
     *
     * Example:
     * - Principal: 10,000
     * - Interest Rate: 10% (0.10)
     * - Duration: 2 months
     *
     * Calculation:
     * - Interest = 10,000 × 0.10 = 1,000
     * - Total Repayment = 10,000 + 1,000 = 11,000
     * - Weeks = 2 months × 4 weeks/month = 8 weeks
     * - Weekly Installment = 11,000 / 8 = 1,375
     */
    public BigDecimal calculateWeeklyRepayment(BigDecimal principal, BigDecimal interestRate,
                                                Integer duration, Loan.DurationUnit unit) {
        // Calculate interest amount
        BigDecimal interestAmount = principal.multiply(interestRate)
                .setScale(2, RoundingMode.HALF_UP);

        // Calculate total repayment
        BigDecimal totalRepayment = principal.add(interestAmount);

        // Convert duration to weeks
        int weeks = convertToWeeks(duration, unit);

        // Calculate weekly installment
        BigDecimal weeklyRepayment = totalRepayment
                .divide(BigDecimal.valueOf(weeks), 2, RoundingMode.HALF_UP);

        log.info("Repayment Calculation: Principal={}, Interest={}, Total={}, Weeks={}, Weekly={}",
                principal, interestAmount, totalRepayment, weeks, weeklyRepayment);

        return weeklyRepayment;
    }

    /**
     * Convert duration to weeks
     * - If unit is WEEKS: return as is
     * - If unit is MONTHS: multiply by 4 (1 month = 4 weeks)
     */
    public int convertToWeeks(Integer duration, Loan.DurationUnit unit) {
        if (unit == Loan.DurationUnit.WEEKS) {
            return duration;
        } else if (unit == Loan.DurationUnit.MONTHS) {
            return duration * 4; // 1 month = 4 weeks
        }
        throw new RuntimeException("Invalid duration unit: " + unit);
    }

    /**
     * Generate complete repayment schedule
     */
    public List<Map<String, Object>> generateSchedule(Loan loan) {
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal interestRate = loan.getProduct().getInterestRate();

        // Calculate totals
        BigDecimal interestAmount = principal.multiply(interestRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRepayment = principal.add(interestAmount);

        int weeks = convertToWeeks(loan.getDuration(), loan.getDurationUnit());
        BigDecimal weeklyAmount = calculateWeeklyRepayment(principal, interestRate,
                loan.getDuration(), loan.getDurationUnit());

        List<Map<String, Object>> schedule = new ArrayList<>();
        BigDecimal remainingBalance = totalRepayment;

        for (int week = 1; week <= weeks; week++) {
            Map<String, Object> installment = new HashMap<>();
            installment.put("weekNumber", week);
            installment.put("installmentAmount", weeklyAmount);

            // Calculate principal and interest portions
            BigDecimal interestPortion = remainingBalance.multiply(interestRate)
                    .divide(BigDecimal.valueOf(weeks), 2, RoundingMode.HALF_UP);
            BigDecimal principalPortion = weeklyAmount.subtract(interestPortion);

            installment.put("principalPortion", principalPortion);
            installment.put("interestPortion", interestPortion);

            remainingBalance = remainingBalance.subtract(weeklyAmount);
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }

            installment.put("remainingBalance", remainingBalance);

            schedule.add(installment);
        }

        return schedule;
    }

    /**
     * Calculate loan summary
     */
    public Map<String, Object> calculateLoanSummary(BigDecimal principal, BigDecimal interestRate,
                                                     Integer duration, Loan.DurationUnit unit) {
        BigDecimal interestAmount = principal.multiply(interestRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRepayment = principal.add(interestAmount);
        int weeks = convertToWeeks(duration, unit);
        BigDecimal weeklyAmount = totalRepayment
                .divide(BigDecimal.valueOf(weeks), 2, RoundingMode.HALF_UP);

        Map<String, Object> summary = new HashMap<>();
        summary.put("principal", principal);
        summary.put("interestRate", interestRate.multiply(BigDecimal.valueOf(100)) + "%");
        summary.put("interestAmount", interestAmount);
        summary.put("totalRepayment", totalRepayment);
        summary.put("duration", duration);
        summary.put("durationUnit", unit.toString());
        summary.put("durationInWeeks", weeks);
        summary.put("weeklyInstallment", weeklyAmount);
        summary.put("monthlyInstallment", weeklyAmount.multiply(BigDecimal.valueOf(4)));

        return summary;
    }

    /**
     * Calculate what member will pay
     * Returns breakdown for frontend display
     */
    public Map<String, Object> calculateRepaymentBreakdown(Loan loan) {
        Map<String, Object> breakdown = new HashMap<>();

        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal interestRate = loan.getProduct().getInterestRate();
        BigDecimal interestAmount = principal.multiply(interestRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRepayment = principal.add(interestAmount);

        int weeks = convertToWeeks(loan.getDuration(), loan.getDurationUnit());
        BigDecimal weeklyAmount = calculateWeeklyRepayment(principal, interestRate,
                loan.getDuration(), loan.getDurationUnit());

        breakdown.put("loanNumber", loan.getLoanNumber());
        breakdown.put("principal", principal);
        breakdown.put("interestRate", interestRate);
        breakdown.put("interestAmount", interestAmount);
        breakdown.put("totalToRepay", totalRepayment);
        breakdown.put("numberOfWeeks", weeks);
        breakdown.put("weeklyInstallment", weeklyAmount);
        breakdown.put("status", loan.getStatus());

        return breakdown;
    }
}

