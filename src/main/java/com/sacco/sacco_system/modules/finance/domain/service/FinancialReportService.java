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
        LocalDate today = LocalDate.now();

        // 1. Calculate Income Streams (Interest + Fees)
        BigDecimal totalInterest = loanRepository.getTotalInterest() != null
                ? loanRepository.getTotalInterest() : BigDecimal.ZERO;

        BigDecimal totalRegFees = transactionRepository.getTotalAmountByType(Transaction.TransactionType.REGISTRATION_FEE);
        if (totalRegFees == null) totalRegFees = BigDecimal.ZERO;

        // Combine all income sources
        BigDecimal calculatedTotalIncome = totalInterest.add(totalRegFees);

        // 2. Calculate Expenses (Withdrawals)
        // TODO: withdrawalRepository.getTotalWithdrawals() method doesn't exist - setting to ZERO
        BigDecimal totalWithdrawals = BigDecimal.ZERO; // withdrawalRepository.getTotalWithdrawals()

        // In this simple model, expenses = withdrawals.
        BigDecimal calculatedTotalExpenses = totalWithdrawals;

        // 3. Build the Report Object
        FinancialReport report = FinancialReport.builder()
                .reportDate(today)
                .totalMembers(BigDecimal.valueOf(memberRepository.countActiveMembers()))
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
                // âœ… SET NEW CALCULATED FIELDS
                .totalIncome(calculatedTotalIncome)
                .totalExpenses(calculatedTotalExpenses)
                .netIncome(calculatedTotalIncome.subtract(calculatedTotalExpenses))
                .build();

        // 4. Save to DB
        return financialReportRepository.save(report);
    }

    public FinancialReport getTodayReport() {
        // We generate it on the fly so the Dashboard is always "Live" with the latest transaction data
        return generateDailyReport();
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





