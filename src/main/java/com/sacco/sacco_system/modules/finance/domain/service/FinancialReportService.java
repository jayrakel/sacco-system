package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.finance.domain.entity.FinancialReport;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import com.sacco.sacco_system.modules.finance.domain.repository.FinancialReportRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.ShareCapitalRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.WithdrawalRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class FinancialReportService {

    private final FinancialReportRepository financialReportRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final ShareCapitalRepository shareCapitalRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Generates a snapshot of the system's financial health for the current day.
     * Includes calculations for Total Income, Expenses, and Net Income.
     */
    public FinancialReport generateDailyReport() {
        System.out.println("📊 [FinancialReportService] Generating daily report...");
        LocalDate today = LocalDate.now();

        try {
            // 1. Calculate Income Streams (Interest + Fees)
            System.out.println("   ↳ Calculating total interest...");
            BigDecimal totalInterest = loanRepository.getTotalInterest() != null
                    ? loanRepository.getTotalInterest() : BigDecimal.ZERO;
            System.out.println("      Total Interest: " + totalInterest);

            System.out.println("   ↳ Calculating registration fees...");
            BigDecimal totalRegFees = transactionRepository.getTotalAmountByType(Transaction.TransactionType.REGISTRATION_FEE);
            if (totalRegFees == null) totalRegFees = BigDecimal.ZERO;
            System.out.println("      Total Reg Fees: " + totalRegFees);

            // Combine all income sources
            BigDecimal calculatedTotalIncome = totalInterest.add(totalRegFees);
            System.out.println("   ↳ Total Income: " + calculatedTotalIncome);

            // 2. Calculate Expenses (Withdrawals)
            System.out.println("   ↳ Calculating withdrawals...");
            // TODO: withdrawalRepository.getTotalWithdrawals() method doesn't exist - setting to ZERO
            BigDecimal totalWithdrawals = BigDecimal.ZERO; // withdrawalRepository.getTotalWithdrawals()
            System.out.println("      Total Withdrawals: " + totalWithdrawals);

            // In this simple model, expenses = withdrawals.
            BigDecimal calculatedTotalExpenses = totalWithdrawals;

            // 3. Build the Report Object
            System.out.println("   ↳ Counting active members...");
            long memberCount = memberRepository.countActiveMembers();
            System.out.println("      Active Members: " + memberCount);

            System.out.println("   ↳ Building report object...");
            FinancialReport report = FinancialReport.builder()
                    .reportDate(today)
                    .totalMembers(BigDecimal.valueOf(memberCount))
                    .totalSavings(savingsAccountRepository.getTotalActiveAccountsBalance() != null
                            ? savingsAccountRepository.getTotalActiveAccountsBalance() : BigDecimal.ZERO)
                    .totalLoansIssued(loanRepository.getTotalDisbursedLoans() != null
                            ? loanRepository.getTotalDisbursedLoans() : BigDecimal.ZERO)
                    .totalLoansOutstanding(loanRepository.getTotalOutstandingLoans() != null
                            ? loanRepository.getTotalOutstandingLoans() : BigDecimal.ZERO)
                    .totalRepayments(loanRepaymentRepository.getTotalRepaidAmount() != null
                            ? loanRepaymentRepository.getTotalRepaidAmount() : BigDecimal.ZERO)
                    .totalInterestCollected(totalInterest)
                    .totalShareCapital(shareCapitalRepository.getTotalShareCapital() != null
                            ? shareCapitalRepository.getTotalShareCapital() : BigDecimal.ZERO)
                    .totalWithdrawals(totalWithdrawals)
                    // ✅ SET NEW CALCULATED FIELDS
                    .totalIncome(calculatedTotalIncome)
                    .totalExpenses(calculatedTotalExpenses)
                    .netIncome(calculatedTotalIncome.subtract(calculatedTotalExpenses))
                    .build();

            // 4. Save to DB
            System.out.println("   ↳ Saving report to database...");
            FinancialReport savedReport = financialReportRepository.save(report);
            System.out.println("✅ [FinancialReportService] Report saved successfully!");
            return savedReport;
        } catch (Exception e) {
            System.err.println("❌ [FinancialReportService] Error in generateDailyReport:");
            System.err.println("   Error Type: " + e.getClass().getName());
            System.err.println("   Error Message: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw so getTodayReport can handle it
        }
    }

    public FinancialReport getTodayReport() {
        try {
            // We generate it on the fly so the Dashboard is always "Live" with the latest transaction data
            return generateDailyReport();
        } catch (Exception e) {
            // Log the error and return a default report with zeros
            System.err.println("Error generating today's report: " + e.getMessage());
            e.printStackTrace();

            // Return a minimal report with zeros to prevent 500 error
            LocalDate today = LocalDate.now();
            return FinancialReport.builder()
                    .reportDate(today)
                    .totalMembers(BigDecimal.ZERO)
                    .totalSavings(BigDecimal.ZERO)
                    .totalLoansIssued(BigDecimal.ZERO)
                    .totalLoansOutstanding(BigDecimal.ZERO)
                    .totalRepayments(BigDecimal.ZERO)
                    .totalInterestCollected(BigDecimal.ZERO)
                    .totalShareCapital(BigDecimal.ZERO)
                    .totalWithdrawals(BigDecimal.ZERO)
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpenses(BigDecimal.ZERO)
                    .netIncome(BigDecimal.ZERO)
                    .build();
        }
    }

    public FinancialReport getReportByDate(LocalDate date) {
        return financialReportRepository.findByReportDate(date)
                .orElseThrow(() -> new RuntimeException("Report not found for date: " + date));
    }

    /**
     * Dynamic Chart Data Fetcher (Last N Days)
     */
    public List<FinancialReport> getChartData(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        // Returns sorted ASC (Mon, Tue, Wed...) perfect for charts
        return financialReportRepository.findByReportDateBetweenOrderByReportDateAsc(startDate, endDate);
    }

    /**
     * âœ… NEW: Dynamic Chart Data Fetcher (Custom Range)
     * Supports specific Start Date to End Date filtering
     */
    public List<FinancialReport> getChartDataCustom(LocalDate startDate, LocalDate endDate) {
        return financialReportRepository.findByReportDateBetweenOrderByReportDateAsc(startDate, endDate);
    }
}





