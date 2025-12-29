package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RepaymentScheduleService {

    public BigDecimal calculateWeeklyRepayment(BigDecimal principal, BigDecimal annualRate, Integer duration, Loan.DurationUnit unit) {
        if (principal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal rateDecimal = annualRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalInterest;
        int installments;

        if (unit == Loan.DurationUnit.WEEKS) {
            installments = duration;
            BigDecimal weeklyRate = rateDecimal.divide(BigDecimal.valueOf(52), 8, RoundingMode.HALF_UP);
            totalInterest = principal.multiply(weeklyRate).multiply(BigDecimal.valueOf(duration));
        } else {
            // Months
            installments = duration;
            BigDecimal monthlyRate = rateDecimal.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
            totalInterest = principal.multiply(monthlyRate).multiply(BigDecimal.valueOf(duration));
        }

        BigDecimal totalPayable = principal.add(totalInterest);
        return totalPayable.divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);
    }
}