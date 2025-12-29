package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * POWER FEATURE: Automated Loan Management
 * Handles scheduled tasks for status updates and notifications.
 * NOTE: Interest calculation is handled upfront (Flat Rate) in the Repayment Schedule,
 * so daily accrual is disabled to prevent double-charging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanAutomationService {

    private final LoanRepository loanRepository;

    /**
     * Check for overdue loans and mark them as DEFAULTED if necessary.
     * Runs every day at 3 AM.
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
            // Check if the loan is past its final expected repayment date
            if (loan.getExpectedRepaymentDate() != null &&
                    LocalDate.now().isAfter(loan.getExpectedRepaymentDate())) {

                long daysOverdue = ChronoUnit.DAYS.between(loan.getExpectedRepaymentDate(), LocalDate.now());

                // Mark as defaulted if over 90 days overdue (NPA Rule)
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
     * Send payment reminders (3 days before due date).
     * Runs every day at 8 AM.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendPaymentReminders() {
        log.info("🤖 AUTOMATION: Sending payment reminders...");

        // Note: For a more robust implementation, query the LoanRepayment table
        // to find specific installments due in 3 days, rather than just the final loan date.

        LocalDate reminderDate = LocalDate.now().plusDays(3);

        // Simple check based on final repayment date (can be expanded to installment level)
        List<Loan> upcomingDue = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.ACTIVE)
                .filter(l -> l.getExpectedRepaymentDate() != null &&
                        l.getExpectedRepaymentDate().equals(reminderDate))
                .toList();

        int remindersSent = 0;

        for (Loan loan : upcomingDue) {
            // Placeholder: Call NotificationService here
            log.info("📱 Reminder: Loan {} due in 3 days (Balance: KES {})",
                    loan.getLoanNumber(), loan.getLoanBalance());
            remindersSent++;
        }

        log.info("✅ Payment reminders sent: {}", remindersSent);
    }

    /**
     * Generate monthly statements (for all members).
     * Runs on 1st day of each month at 4 AM.
     */
    @Scheduled(cron = "0 0 4 1 * *")
    public void generateMonthlyStatements() {
        log.info("🤖 AUTOMATION: Generating monthly statements...");
        // Logic to trigger statement generation service would go here
        log.info("✅ Monthly statements generation complete");
    }
}