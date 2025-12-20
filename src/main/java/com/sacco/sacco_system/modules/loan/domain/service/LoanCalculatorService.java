package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POWER FEATURE: Advanced Loan Calculator
 * Generates amortization schedules, calculates payments, early repayment, etc.
 */
@Service
public class LoanCalculatorService {

    /**
     * Calculate monthly payment for a loan (Principal + Interest)
     */
    public BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (months == 0) return principal;

        // Convert annual rate to monthly
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            // No interest - just divide principal
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        // Formula: M = P [ i(1 + i)^n ] / [ (1 + i)^n – 1]
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusRate.pow(months);

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * Generate full amortization schedule
     */
    public List<PaymentScheduleItem> generateAmortizationSchedule(
            BigDecimal principal,
            BigDecimal annualRate,
            int months,
            LocalDate startDate) {

        List<PaymentScheduleItem> schedule = new ArrayList<>();

        BigDecimal monthlyPayment = calculateMonthlyPayment(principal, annualRate, months);
        BigDecimal balance = principal;

        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        for (int month = 1; month <= months; month++) {
            // Calculate interest for this month
            BigDecimal interestPayment = balance.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);

            // Principal payment = monthly payment - interest
            BigDecimal principalPayment = monthlyPayment.subtract(interestPayment);

            // Adjust last payment to account for rounding
            if (month == months) {
                principalPayment = balance;
                monthlyPayment = principalPayment.add(interestPayment);
            }

            // New balance
            BigDecimal newBalance = balance.subtract(principalPayment);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                newBalance = BigDecimal.ZERO;
            }

            PaymentScheduleItem item = PaymentScheduleItem.builder()
                    .paymentNumber(month)
                    .paymentDate(startDate.plusMonths(month))
                    .paymentAmount(monthlyPayment)
                    .principalAmount(principalPayment)
                    .interestAmount(interestPayment)
                    .balance(newBalance)
                    .build();

            schedule.add(item);
            balance = newBalance;
        }

        return schedule;
    }

    /**
     * Calculate total interest to be paid over loan life
     */
    public BigDecimal calculateTotalInterest(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal monthlyPayment = calculateMonthlyPayment(principal, annualRate, months);
        BigDecimal totalPayments = monthlyPayment.multiply(BigDecimal.valueOf(months));
        return totalPayments.subtract(principal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate early repayment amount (with penalty if applicable)
     */
    public Map<String, BigDecimal> calculateEarlyRepayment(
            Loan loan,
            LocalDate repaymentDate,
            BigDecimal penaltyRate) {

        Map<String, BigDecimal> result = new HashMap<>();

        BigDecimal outstandingBalance = loan.getLoanBalance() != null ?
                loan.getLoanBalance() : loan.getPrincipalAmount();

        // Calculate penalty (e.g., 2% of outstanding balance)
        BigDecimal penalty = outstandingBalance
                .multiply(penaltyRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);

        // Total to pay
        BigDecimal totalPayment = outstandingBalance.add(penalty);

        // Calculate interest saved (remaining months)
        LocalDate today = LocalDate.now();
        LocalDate endDate = loan.getExpectedRepaymentDate();

        // Simple interest saved calculation
        BigDecimal interestSaved = BigDecimal.ZERO;
        if (endDate != null && endDate.isAfter(repaymentDate)) {
            int remainingMonths = (int) java.time.temporal.ChronoUnit.MONTHS.between(repaymentDate, endDate);
            BigDecimal totalInterest = calculateTotalInterest(
                    loan.getPrincipalAmount(),
                    loan.getProduct().getInterestRate(),
                    loan.getDuration());
            interestSaved = totalInterest
                    .multiply(BigDecimal.valueOf(remainingMonths))
                    .divide(BigDecimal.valueOf(loan.getDuration()), 2, RoundingMode.HALF_UP);
        }

        result.put("outstandingBalance", outstandingBalance);
        result.put("earlyRepaymentPenalty", penalty);
        result.put("totalPaymentRequired", totalPayment);
        result.put("interestSaved", interestSaved);
        result.put("netSavings", interestSaved.subtract(penalty));

        return result;
    }

    /**
     * Calculate loan affordability (what member can afford)
     */
    public Map<String, Object> calculateAffordability(
            BigDecimal monthlyIncome,
            BigDecimal existingMonthlyObligations,
            BigDecimal maxDebtRatio, // e.g., 40% of income
            BigDecimal annualRate,
            int months) {

        Map<String, Object> result = new HashMap<>();

        // Maximum affordable monthly payment
        BigDecimal maxMonthlyPayment = monthlyIncome
                .multiply(maxDebtRatio.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                .subtract(existingMonthlyObligations)
                .setScale(2, RoundingMode.HALF_UP);

        if (maxMonthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
            result.put("canAfford", false);
            result.put("maxLoanAmount", BigDecimal.ZERO);
            result.put("maxMonthlyPayment", BigDecimal.ZERO);
            result.put("reason", "Existing obligations exceed affordable debt ratio");
            return result;
        }

        // Calculate maximum loan principal that this payment can service
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        // Reverse calculation: P = M * [(1 + i)^n - 1] / [i(1 + i)^n]
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusRate.pow(months);

        BigDecimal maxPrincipal = maxMonthlyPayment
                .multiply(power.subtract(BigDecimal.ONE))
                .divide(monthlyRate.multiply(power), 2, RoundingMode.HALF_UP);

        result.put("canAfford", true);
        result.put("maxLoanAmount", maxPrincipal);
        result.put("maxMonthlyPayment", maxMonthlyPayment);
        result.put("totalInterest", calculateTotalInterest(maxPrincipal, annualRate, months));
        result.put("totalRepayment", maxMonthlyPayment.multiply(BigDecimal.valueOf(months)));

        return result;
    }

    /**
     * Compare different loan terms
     */
    public List<Map<String, Object>> compareLoanOptions(
            BigDecimal principal,
            BigDecimal annualRate,
            List<Integer> termOptions) {

        List<Map<String, Object>> comparisons = new ArrayList<>();

        for (Integer months : termOptions) {
            Map<String, Object> option = new HashMap<>();

            BigDecimal monthlyPayment = calculateMonthlyPayment(principal, annualRate, months);
            BigDecimal totalInterest = calculateTotalInterest(principal, annualRate, months);
            BigDecimal totalPayment = monthlyPayment.multiply(BigDecimal.valueOf(months));

            option.put("term", months + " months");
            option.put("monthlyPayment", monthlyPayment);
            option.put("totalInterest", totalInterest);
            option.put("totalPayment", totalPayment);
            option.put("interestPercentage", totalInterest
                    .divide(principal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));

            comparisons.add(option);
        }

        return comparisons;
    }

    // ============================================================================
    // INNER CLASSES
    // ============================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentScheduleItem {
        private Integer paymentNumber;
        private LocalDate paymentDate;
        private BigDecimal paymentAmount;
        private BigDecimal principalAmount;
        private BigDecimal interestAmount;
        private BigDecimal balance;
    }
}

