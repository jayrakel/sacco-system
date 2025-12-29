package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Repayment Schedule Service
 * Calculates repayment amounts and schedules for loans
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepaymentScheduleService {

    /**
     * Calculate weekly repayment amount for a loan
     *
     * @param principalAmount The loan principal
     * @param annualInterestRate Annual interest rate as percentage (e.g., 10 for 10%)
     * @param duration Number of periods
     * @param durationUnit WEEKS or MONTHS
     * @return Weekly repayment amount
     */
    public BigDecimal calculateWeeklyRepayment(
            BigDecimal principalAmount,
            BigDecimal annualInterestRate,
            Integer duration,
            Loan.DurationUnit durationUnit) {

        if (principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal amount must be positive");
        }

        if (duration == null || duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        // Convert annual rate to decimal (e.g., 10% -> 0.10)
        BigDecimal rateDecimal = annualInterestRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        // Calculate total interest based on duration unit
        BigDecimal totalInterest;
        int totalWeeks;

        if (durationUnit == Loan.DurationUnit.WEEKS) {
            // Weekly interest rate = annual rate / 52
            BigDecimal weeklyRate = rateDecimal.divide(BigDecimal.valueOf(52), 8, RoundingMode.HALF_UP);
            totalInterest = principalAmount.multiply(weeklyRate).multiply(BigDecimal.valueOf(duration));
            totalWeeks = duration;
        } else {
            // Monthly - convert to weeks (1 month ≈ 4.33 weeks)
            // Monthly interest rate = annual rate / 12
            BigDecimal monthlyRate = rateDecimal.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
            totalInterest = principalAmount.multiply(monthlyRate).multiply(BigDecimal.valueOf(duration));
            totalWeeks = (int) Math.ceil(duration * 4.33); // Convert months to weeks
        }

        // Total amount to repay = principal + interest
        BigDecimal totalAmount = principalAmount.add(totalInterest);

        // Weekly repayment = total amount / total weeks
        BigDecimal weeklyRepayment = totalAmount.divide(BigDecimal.valueOf(totalWeeks), 2, RoundingMode.HALF_UP);

        log.debug("Calculated weekly repayment: Principal={}, Rate={}%, Duration={} {}, TotalInterest={}, WeeklyPayment={}",
                principalAmount, annualInterestRate, duration, durationUnit, totalInterest, weeklyRepayment);

        return weeklyRepayment;
    }

    /**
     * Calculate monthly repayment amount for a loan
     *
     * @param principalAmount The loan principal
     * @param annualInterestRate Annual interest rate as percentage
     * @param duration Number of periods
     * @param durationUnit WEEKS or MONTHS
     * @return Monthly repayment amount
     */
    public BigDecimal calculateMonthlyRepayment(
            BigDecimal principalAmount,
            BigDecimal annualInterestRate,
            Integer duration,
            Loan.DurationUnit durationUnit) {

        // Convert annual rate to decimal
        BigDecimal rateDecimal = annualInterestRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        // Calculate total interest
        BigDecimal totalInterest;
        int totalMonths;

        if (durationUnit == Loan.DurationUnit.MONTHS) {
            // Monthly interest rate = annual rate / 12
            BigDecimal monthlyRate = rateDecimal.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
            totalInterest = principalAmount.multiply(monthlyRate).multiply(BigDecimal.valueOf(duration));
            totalMonths = duration;
        } else {
            // Weeks - convert to months (4.33 weeks ≈ 1 month)
            totalMonths = (int) Math.ceil(duration / 4.33);
            BigDecimal weeklyRate = rateDecimal.divide(BigDecimal.valueOf(52), 8, RoundingMode.HALF_UP);
            totalInterest = principalAmount.multiply(weeklyRate).multiply(BigDecimal.valueOf(duration));
        }

        // Total amount to repay = principal + interest
        BigDecimal totalAmount = principalAmount.add(totalInterest);

        // Monthly repayment = total amount / total months
        BigDecimal monthlyRepayment = totalAmount.divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);

        return monthlyRepayment;
    }

    /**
     * Calculate total interest for a loan
     *
     * @param principalAmount The loan principal
     * @param annualInterestRate Annual interest rate as percentage
     * @param duration Number of periods
     * @param durationUnit WEEKS or MONTHS
     * @return Total interest amount
     */
    public BigDecimal calculateTotalInterest(
            BigDecimal principalAmount,
            BigDecimal annualInterestRate,
            Integer duration,
            Loan.DurationUnit durationUnit) {

        BigDecimal rateDecimal = annualInterestRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        if (durationUnit == Loan.DurationUnit.WEEKS) {
            BigDecimal weeklyRate = rateDecimal.divide(BigDecimal.valueOf(52), 8, RoundingMode.HALF_UP);
            return principalAmount.multiply(weeklyRate).multiply(BigDecimal.valueOf(duration));
        } else {
            BigDecimal monthlyRate = rateDecimal.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
            return principalAmount.multiply(monthlyRate).multiply(BigDecimal.valueOf(duration));
        }
    }

    public void generateSchedule(Loan activeLoan) {
    }
}

