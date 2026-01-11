package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentScheduleRepository; // ✅ IMPORT ADDED
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanDailyProcessor {

    private final LoanRepository loanRepository;
    private final AccountingService accountingService;
    private final SystemSettingService systemSettingService;
    private final LoanRepaymentScheduleRepository scheduleRepository; // ✅ INJECTED

    /**
     * ✅ CRON: Runs every day at 00:01 AM
     * Performs: Interest Accrual, Arrears Check, Penalty Application
     */
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void runDailyLoanProcessing() {
        log.info("⏰ Starting Daily Loan Processing...");

        List<Loan> activeLoans = loanRepository.findByLoanStatusIn(
                List.of(Loan.LoanStatus.DISBURSED, Loan.LoanStatus.ACTIVE, Loan.LoanStatus.IN_ARREARS)
        );

        int interestCount = 0;
        int penaltyCount = 0;
        int statusCount = 0;

        for (Loan loan : activeLoans) {
            // 1. Accrue Daily Interest (Domain Rule Section 22)
            if (accrueInterest(loan)) {
                interestCount++;
            }

            // 2. Check and Apply Penalties (Domain Rule Section 18)
            if (checkAndApplyPenalties(loan)) {
                penaltyCount++;
            }

            // 3. Update Loan Status (Derived State)
            if (updateLoanStatus(loan)) {
                statusCount++;
            }

            loanRepository.save(loan);
        }

        log.info("✅ Daily Processing Complete: {} Interest Accrued, {} Penalties Applied, {} Statuses Updated.",
                interestCount, penaltyCount, statusCount);
    }

    /**
     * Logic 1: Accrue Interest
     * Formula: (Outstanding Principal * Annual Rate) / 365
     */
    private boolean accrueInterest(Loan loan) {
        if (loan.getInterestRate() == null || loan.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) return false;

        // Annual Rate / 100 / 365
        BigDecimal dailyRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

        BigDecimal dailyInterest = loan.getOutstandingPrincipal()
                .multiply(dailyRate)
                .setScale(2, RoundingMode.HALF_UP);

        if (dailyInterest.compareTo(BigDecimal.ZERO) > 0) {
            // Update Loan State
            loan.setOutstandingInterest(loan.getOutstandingInterest().add(dailyInterest));
            loan.setTotalOutstandingAmount(loan.getTotalOutstandingAmount().add(dailyInterest));

            // 📝 POST TO ACCOUNTING (Accrual Accounting)
            // Dr Loan Receivable (Asset)
            // Cr Interest Income (Income)
            accountingService.postEvent(
                    "INTEREST_ACCRUAL",
                    "Daily Interest - " + loan.getLoanNumber(),
                    loan.getId().toString(),
                    dailyInterest,
                    "SYSTEM"
            );
            return true;
        }
        return false;
    }

    /**
     * Logic 2: Apply Penalties on Overdue Loans
     */
    private boolean checkAndApplyPenalties(Loan loan) {
        // Calculate true arrears from schedule
        BigDecimal arrears = calculateArrears(loan);

        // If no arrears, no penalty
        if (arrears.compareTo(BigDecimal.ZERO) <= 0) return false;

        // Check if penalty rate exists
        BigDecimal penaltyRate = loan.getProduct().getPenaltyRate(); // e.g., 10%
        if (penaltyRate == null || penaltyRate.compareTo(BigDecimal.ZERO) == 0) return false;

        // Penalty on the Arrears Amount (not total loan)
        BigDecimal penalty = arrears.multiply(penaltyRate.divide(BigDecimal.valueOf(100)))
                .setScale(2, RoundingMode.HALF_UP);

        if (penalty.compareTo(BigDecimal.ZERO) > 0) {
            loan.setTotalOutstandingAmount(loan.getTotalOutstandingAmount().add(penalty));

            // 📝 POST TO ACCOUNTING
            // Dr Loan Receivable
            // Cr Penalty Income
            accountingService.postEvent(
                    "PENALTY_APPLIED",
                    "Late Penalty - " + loan.getLoanNumber(),
                    loan.getId().toString(),
                    penalty,
                    "SYSTEM"
            );
            return true;
        }
        return false;
    }

    /**
     * Logic 3: Update Status (Active -> In Arrears)
     */
    private boolean updateLoanStatus(Loan loan) {
        BigDecimal arrears = calculateArrears(loan);
        Loan.LoanStatus oldStatus = loan.getLoanStatus();
        Loan.LoanStatus newStatus = oldStatus;

        if (arrears.compareTo(BigDecimal.ZERO) > 0) {
            newStatus = Loan.LoanStatus.IN_ARREARS;
        } else if (loan.getTotalOutstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
            newStatus = Loan.LoanStatus.ACTIVE;
        }

        if (oldStatus != newStatus) {
            loan.setLoanStatus(newStatus);
            return true;
        }
        return false;
    }

    /**
     * Helper: Calculate Arrears (Derived State)
     * ✅ FIXED: Uses LoanRepaymentScheduleRepository to find real overdue amounts
     */
    private BigDecimal calculateArrears(Loan loan) {
        // Sum of (TotalDue - PaidAmount) for all installments where DueDate < Today
        return scheduleRepository.calculateTotalArrears(loan.getId(), LocalDate.now());
    }
}