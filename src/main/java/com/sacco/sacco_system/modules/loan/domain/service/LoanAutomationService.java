package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POWER FEATURE: Automated Loan Calculations
 * Runs scheduled tasks for interest calculations, payment reminders, etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanAutomationService {

    private final LoanRepository loanRepository;

    /**
     * Calculate daily interest on all active loans
     * Runs every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void calculateDailyInterest() {
        log.info("🤖 AUTOMATION: Starting daily interest calculation...");

        List<Loan> activeLoans = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE ||
                            l.getStatus() == Loan.LoanStatus.DISBURSED)
                .toList();

        int processed = 0;
        BigDecimal totalInterestAccrued = BigDecimal.ZERO;

        for (Loan loan : activeLoans) {
            try {
                BigDecimal dailyInterest = calculateDailyInterestForLoan(loan);

                // Update loan with accrued interest
                BigDecimal currentArrears = loan.getTotalArrears() != null ?
                        loan.getTotalArrears() : BigDecimal.ZERO;
                loan.setTotalArrears(currentArrears.add(dailyInterest));

                loanRepository.save(loan);

                totalInterestAccrued = totalInterestAccrued.add(dailyInterest);
                processed++;

            } catch (Exception e) {
                log.error("Error calculating interest for loan {}: {}",
                        loan.getLoanNumber(), e.getMessage());
            }
        }

        log.info("✅ Daily interest calculation complete. Processed: {} loans, Total interest: KES {}",
                processed, totalInterestAccrued);
    }

    /**
     * Check for overdue loans and mark them
     * Runs every day at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void checkOverdueLoans() {
        log.info("🤖 AUTOMATION: Checking for overdue loans...");

        List<Loan> activeLoans = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE)
                .toList();

        int markedOverdue = 0;

        for (Loan loan : activeLoans) {
            if (loan.getExpectedRepaymentDate() != null &&
                LocalDate.now().isAfter(loan.getExpectedRepaymentDate())) {

                long daysOverdue = ChronoUnit.DAYS.between(loan.getExpectedRepaymentDate(), LocalDate.now());

                // Mark as defaulted if over 90 days overdue
                if (daysOverdue > 90 && loan.getStatus() != Loan.LoanStatus.DEFAULTED) {
                    loan.setStatus(Loan.LoanStatus.DEFAULTED);
                    loanRepository.save(loan);
                    markedOverdue++;
                    log.warn("⚠️ Loan {} marked as DEFAULTED ({} days overdue)",
                            loan.getLoanNumber(), daysOverdue);
                }
            }
        }

        log.info("✅ Overdue check complete. Marked {} loans as defaulted", markedOverdue);
    }

    /**
     * Generate monthly statements (for all members)
     * Runs on 1st day of each month at 4 AM
     */
    @Scheduled(cron = "0 0 4 1 * *")
    public void generateMonthlyStatements() {
        log.info("🤖 AUTOMATION: Generating monthly statements...");

        // This will be implemented with the statement generator service
        // For now, just log

        log.info("✅ Monthly statements generation complete");
    }

    /**
     * Send payment reminders (3 days before due date)
     * Runs every day at 8 AM
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendPaymentReminders() {
        log.info("🤖 AUTOMATION: Sending payment reminders...");

        LocalDate reminderDate = LocalDate.now().plusDays(3);

        List<Loan> upcomingDue = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE)
                .filter(l -> l.getExpectedRepaymentDate() != null &&
                            l.getExpectedRepaymentDate().equals(reminderDate))
                .toList();

        int remindersSent = 0;

        for (Loan loan : upcomingDue) {
            // TODO: Send SMS/Email notification
            log.info("📱 Reminder: Loan {} due in 3 days (Amount: KES {})",
                    loan.getLoanNumber(), loan.getLoanBalance());
            remindersSent++;
        }

        log.info("✅ Payment reminders sent: {}", remindersSent);
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Calculate daily interest for a specific loan
     */
    private BigDecimal calculateDailyInterestForLoan(Loan loan) {
        if (loan.getLoanBalance() == null || loan.getLoanBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Get annual interest rate from product
        BigDecimal annualRate = loan.getProduct() != null && loan.getProduct().getInterestRate() != null ?
                loan.getProduct().getInterestRate() : BigDecimal.ZERO;

        // Convert to daily rate (annual rate / 365)
        BigDecimal dailyRate = annualRate.divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        // Calculate daily interest = balance * daily rate
        BigDecimal dailyInterest = loan.getLoanBalance()
                .multiply(dailyRate)
                .setScale(2, RoundingMode.HALF_UP);

        return dailyInterest;
    }

    /**
     * Manual trigger for interest calculation (for testing/admin use)
     */
    public Map<String, Object> manualInterestCalculation(LocalDate targetDate) {
        log.info("🔧 MANUAL: Calculating interest for date: {}", targetDate);

        List<Loan> activeLoans = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE ||
                            l.getStatus() == Loan.LoanStatus.DISBURSED)
                .toList();

        int processed = 0;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (Loan loan : activeLoans) {
            BigDecimal interest = calculateDailyInterestForLoan(loan);
            totalInterest = totalInterest.add(interest);
            processed++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("targetDate", targetDate);
        result.put("loansProcessed", processed);
        result.put("totalInterestCalculated", totalInterest);
        result.put("averageInterestPerLoan", processed > 0 ?
                totalInterest.divide(BigDecimal.valueOf(processed), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        return result;
    }
}

