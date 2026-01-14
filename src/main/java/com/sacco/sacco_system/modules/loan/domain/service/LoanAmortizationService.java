package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanAmortizationService {

    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final SystemSettingRepository systemSettingRepository;

    @Transactional
    public void generateSchedule(Loan loan) {
        // Clear existing schedule
        List<LoanRepaymentSchedule> existing = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
        if (!existing.isEmpty()) {
            scheduleRepository.deleteAll(existing);
        }

        // 1. Fetch Grace Period (Default to 0 if not set)
        int gracePeriodWeeks = 0;
        Optional<SystemSetting> setting = systemSettingRepository.findByKey("LOAN_GRACE_PERIOD_WEEKS");
        if (setting.isPresent()) {
            try {
                gracePeriodWeeks = Integer.parseInt(setting.get().getValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid Grace Period setting. Defaulting to 0.");
            }
        }

        // 2. Generate Schedule
        LoanProduct.InterestType interestType = loan.getProduct().getInterestType();
        if (interestType == null || interestType == LoanProduct.InterestType.FLAT) {
            generateFlatRateSchedule(loan, gracePeriodWeeks);
        } else {
            generateReducingBalanceSchedule(loan, gracePeriodWeeks);
        }
    }

    private void generateFlatRateSchedule(Loan loan, int graceWeeks) {
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal rate = loan.getInterestRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        int weeks = loan.getDurationWeeks();

        BigDecimal totalInterest = principal.multiply(rate)
                .multiply(BigDecimal.valueOf(weeks))
                .divide(BigDecimal.valueOf(52), 2, RoundingMode.HALF_UP);

        BigDecimal weeklyPrincipal = principal.divide(BigDecimal.valueOf(weeks), 2, RoundingMode.HALF_UP);
        BigDecimal weeklyInterest = totalInterest.divide(BigDecimal.valueOf(weeks), 2, RoundingMode.HALF_UP);

        BigDecimal calculatedTotalPrincipal = weeklyPrincipal.multiply(BigDecimal.valueOf(weeks));
        BigDecimal principalDiff = principal.subtract(calculatedTotalPrincipal);

        createInstallments(loan, weeks, weeklyPrincipal, weeklyInterest, principalDiff, graceWeeks);
    }

    private void generateReducingBalanceSchedule(Loan loan, int graceWeeks) {
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal annualRate = loan.getInterestRate().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        int weeks = loan.getDurationWeeks();

        BigDecimal periodicRate = annualRate.divide(BigDecimal.valueOf(52), 8, RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(periodicRate);
        BigDecimal powerFactor = onePlusR.pow(weeks);

        BigDecimal numerator = principal.multiply(periodicRate).multiply(powerFactor);
        BigDecimal denominator = powerFactor.subtract(BigDecimal.ONE);
        BigDecimal weeklyPayment = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

        createAmortizedInstallments(loan, weeks, weeklyPayment, periodicRate, principal, graceWeeks);
    }

    private void createInstallments(Loan loan, int weeks, BigDecimal principalPerInstallment, BigDecimal interestPerInstallment, BigDecimal principalAdjustment, int graceWeeks) {
        LocalDate disbursementDate = loan.getDisbursementDate() != null ? loan.getDisbursementDate() : LocalDate.now();

        // ✅ FIX: Adjust Start Offset
        // If Grace=4: StartOffset = 3.
        // Week 1 Due = Disbursement + (1 + 3) = 4 Weeks. (Correct)
        // If Grace=0: StartOffset = 0.
        // Week 1 Due = Disbursement + (1 + 0) = 1 Week. (Correct)
        int startOffset = (graceWeeks > 0) ? graceWeeks - 1 : 0;

        for (int i = 1; i <= weeks; i++) {
            BigDecimal currentPrincipal = principalPerInstallment;
            if (i == weeks) currentPrincipal = currentPrincipal.add(principalAdjustment);

            LocalDate dueDate = disbursementDate.plusWeeks(i + startOffset);

            LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .dueDate(dueDate)
                    .principalDue(currentPrincipal)
                    .interestDue(interestPerInstallment)
                    .totalDue(currentPrincipal.add(interestPerInstallment))
                    .paidAmount(BigDecimal.ZERO)
                    .status(LoanRepaymentSchedule.InstallmentStatus.PENDING)
                    .build();
            scheduleRepository.save(schedule);
        }
    }

    private void createAmortizedInstallments(Loan loan, int weeks, BigDecimal fixedPayment, BigDecimal periodicRate, BigDecimal totalPrincipal, int graceWeeks) {
        LocalDate disbursementDate = loan.getDisbursementDate() != null ? loan.getDisbursementDate() : LocalDate.now();

        // ✅ FIX: Same offset logic for reducing balance
        int startOffset = (graceWeeks > 0) ? graceWeeks - 1 : 0;

        BigDecimal balance = totalPrincipal;

        for (int i = 1; i <= weeks; i++) {
            BigDecimal interestPart = balance.multiply(periodicRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart = fixedPayment.subtract(interestPart);

            if (i == weeks || principalPart.compareTo(balance) > 0) {
                principalPart = balance;
                fixedPayment = principalPart.add(interestPart);
            }

            LocalDate dueDate = disbursementDate.plusWeeks(i + startOffset);

            LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .dueDate(dueDate)
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