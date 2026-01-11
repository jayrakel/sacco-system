package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepaymentSchedule;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanAmortizationService {

    private final LoanRepaymentScheduleRepository scheduleRepository;

    /**
     * ✅ Generates the repayment schedule for a newly disbursed loan
     */
    @Transactional
    public void generateSchedule(Loan loan) {
        // Clear existing if any (Safe guard)
        List<LoanRepaymentSchedule> existing = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
        if (!existing.isEmpty()) {
            scheduleRepository.deleteAll(existing);
        }

        // ✅ FIX: Use getProduct() instead of getLoanProduct()
        LoanProduct.InterestType interestType = loan.getProduct().getInterestType();

        // Default to FLAT if null, otherwise switch based on type
        if (interestType == null || interestType == LoanProduct.InterestType.FLAT) {
            generateFlatRateSchedule(loan);
        } else {
            generateReducingBalanceSchedule(loan);
        }

        log.info("Generated Repayment Schedule for Loan: {}", loan.getLoanNumber());
    }

    private void generateFlatRateSchedule(Loan loan) {
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal rate = loan.getInterestRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        int weeks = loan.getDurationWeeks();

        // Total Interest = P * R * (T in years) -> (P * R * Weeks) / 52
        BigDecimal totalInterest = principal.multiply(rate)
                .multiply(BigDecimal.valueOf(weeks))
                .divide(BigDecimal.valueOf(52), 2, RoundingMode.HALF_UP);

        BigDecimal weeklyPrincipal = principal.divide(BigDecimal.valueOf(weeks), 2, RoundingMode.HALF_UP);
        BigDecimal weeklyInterest = totalInterest.divide(BigDecimal.valueOf(weeks), 2, RoundingMode.HALF_UP);

        // Correct rounding errors on the last installment
        BigDecimal calculatedTotalPrincipal = weeklyPrincipal.multiply(BigDecimal.valueOf(weeks));
        BigDecimal principalDiff = principal.subtract(calculatedTotalPrincipal);

        createInstallments(loan, weeks, weeklyPrincipal, weeklyInterest, principalDiff);
    }

    private void generateReducingBalanceSchedule(Loan loan) {
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal annualRate = loan.getInterestRate().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        int weeks = loan.getDurationWeeks();

        // Weekly Rate = Annual / 52
        BigDecimal periodicRate = annualRate.divide(BigDecimal.valueOf(52), 8, RoundingMode.HALF_UP);

        // Amortization Formula: PMT = [P * r * (1+r)^n] / [(1+r)^n - 1]
        BigDecimal onePlusR = BigDecimal.ONE.add(periodicRate);
        BigDecimal powerFactor = onePlusR.pow(weeks);

        BigDecimal numerator = principal.multiply(periodicRate).multiply(powerFactor);
        BigDecimal denominator = powerFactor.subtract(BigDecimal.ONE);

        BigDecimal weeklyPayment = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

        createAmortizedInstallments(loan, weeks, weeklyPayment, periodicRate, principal);
    }

    private void createInstallments(Loan loan, int weeks, BigDecimal principalPerInstallment, BigDecimal interestPerInstallment, BigDecimal principalAdjustment) {
        LocalDate startDate = loan.getDisbursementDate() != null ? loan.getDisbursementDate() : LocalDate.now();

        for (int i = 1; i <= weeks; i++) {
            BigDecimal currentPrincipal = principalPerInstallment;

            // Add rounding difference to last installment
            if (i == weeks) {
                currentPrincipal = currentPrincipal.add(principalAdjustment);
            }

            LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .dueDate(startDate.plusWeeks(i))
                    .principalDue(currentPrincipal)
                    .interestDue(interestPerInstallment)
                    .totalDue(currentPrincipal.add(interestPerInstallment))
                    .paidAmount(BigDecimal.ZERO)
                    .status(LoanRepaymentSchedule.InstallmentStatus.PENDING)
                    .build();
            scheduleRepository.save(schedule);
        }
    }

    private void createAmortizedInstallments(Loan loan, int weeks, BigDecimal fixedPayment, BigDecimal periodicRate, BigDecimal totalPrincipal) {
        LocalDate startDate = loan.getDisbursementDate() != null ? loan.getDisbursementDate() : LocalDate.now();
        BigDecimal balance = totalPrincipal;

        for (int i = 1; i <= weeks; i++) {
            BigDecimal interestPart = balance.multiply(periodicRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart = fixedPayment.subtract(interestPart);

            // Adjust for last installment rounding
            if (i == weeks || principalPart.compareTo(balance) > 0) {
                principalPart = balance;
                fixedPayment = principalPart.add(interestPart);
            }

            LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .dueDate(startDate.plusWeeks(i))
                    .principalDue(principalPart)
                    .interestDue(interestPart)
                    .totalDue(fixedPayment)
                    .paidAmount(BigDecimal.ZERO)
                    .status(LoanRepaymentSchedule.InstallmentStatus.PENDING)
                    .build();
            scheduleRepository.save(schedule);

            balance = balance.subtract(principalPart);
        }
    }
}