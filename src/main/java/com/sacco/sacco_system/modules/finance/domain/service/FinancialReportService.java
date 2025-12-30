package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.finance.domain.entity.FinancialReport;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.AccountType;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GLAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.sacco.sacco_system.modules.finance.domain.repository.FinancialReportRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.ShareCapitalRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
// ✅ REMOVED: LoanRepaymentRepository (Deleted during cleanup)
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FinancialReportService {

    private final FinancialReportRepository financialReportRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;
    // private final LoanRepaymentRepository loanRepaymentRepository; // Commented out
    private final ShareCapitalRepository shareCapitalRepository;
    private final TransactionRepository transactionRepository;

    // ✅ Use AccountingService for Truth (GL Balances)
    private final AccountingService accountingService;

    /**
     * Generates a snapshot of the system's financial health for the current day.
     * Sourced primarily from the General Ledger to ensure accounting integrity.
     */
    public FinancialReport generateDailyReport() {
        log.info("📊 [FinancialReportService] Generating daily report...");
        LocalDate today = LocalDate.now();

        try {
            // =================================================================================
            // 1. ACCOUNTING DATA (Source: General Ledger)
            // =================================================================================
            List<GLAccount> glAccounts = accountingService.getAccountsWithBalancesAsOf(null, today);

            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpenses = BigDecimal.ZERO;
            BigDecimal totalInterestIncome = BigDecimal.ZERO; // 4002

            for (GLAccount acc : glAccounts) {
                if (acc.getType() == AccountType.INCOME) {
                    totalIncome = totalIncome.add(acc.getBalance());

                    if ("4002".equals(acc.getCode())) {
                        totalInterestIncome = acc.getBalance();
                    }
                }
                else if (acc.getType() == AccountType.EXPENSE) {
                    totalExpenses = totalExpenses.add(acc.getBalance());
                }
            }

            BigDecimal netIncome = totalIncome.subtract(totalExpenses);
            log.info("   ↳ GL Summary: Income={}, Expenses={}, Net={}", totalIncome, totalExpenses, netIncome);

            // =================================================================================
            // 2. OPERATIONAL DATA (Source: Entities)
            // =================================================================================

            long memberCount = memberRepository.countActiveMembers();

            BigDecimal totalWithdrawals = transactionRepository.getTotalAmountByType(Transaction.TransactionType.WITHDRAWAL);
            if (totalWithdrawals == null) totalWithdrawals = BigDecimal.ZERO;

            // ✅ SKELETON MODE: Set complex metrics to ZERO to prevent crashes
            // We will re-enable these queries as we build out the Loan/Savings modules
            BigDecimal totalSavings = BigDecimal.ZERO; // TODO: Add getTotalSystemSavings to SavingsRepo
            BigDecimal totalLoansIssued = BigDecimal.ZERO; // TODO: Add getTotalIssued to LoanRepo
            BigDecimal totalLoansOutstanding = BigDecimal.ZERO; // TODO: Add getTotalOutstanding to LoanRepo
            BigDecimal totalRepayments = BigDecimal.ZERO;
            BigDecimal totalShareCapital = shareCapitalRepository.getTotalShareCapital();
            if (totalShareCapital == null) totalShareCapital = BigDecimal.ZERO;

            // =================================================================================
            // 3. BUILD REPORT
            // =================================================================================
            FinancialReport report = FinancialReport.builder()
                    .reportDate(today)
                    .totalMembers(BigDecimal.valueOf(memberCount))

                    // Balance Sheet Items (Operational View)
                    .totalSavings(totalSavings)
                    .totalLoansIssued(totalLoansIssued)
                    .totalLoansOutstanding(totalLoansOutstanding)
                    .totalRepayments(totalRepayments)
                    .totalShareCapital(totalShareCapital)

                    // ✅ P&L Items (Sourced from GL for accuracy)
                    .totalInterestCollected(totalInterestIncome)
                    .totalIncome(totalIncome)
                    .totalExpenses(totalExpenses)
                    .netIncome(netIncome)

                    .totalWithdrawals(totalWithdrawals)
                    .build();

            // 4. Save to DB
            FinancialReport savedReport = financialReportRepository.save(report);
            log.info("✅ [FinancialReportService] Report saved successfully! ID: {}", savedReport.getId());
            return savedReport;

        } catch (Exception e) {
            log.error("❌ [FinancialReportService] Error generating daily report: {}", e.getMessage(), e);
            throw e;
        }
    }

    public FinancialReport getTodayReport() {
        try {
            return generateDailyReport();
        } catch (Exception e) {
            log.error("Error fetching today's report", e);
            return FinancialReport.builder()
                    .reportDate(LocalDate.now())
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpenses(BigDecimal.ZERO)
                    .netIncome(BigDecimal.ZERO)
                    .totalMembers(BigDecimal.ZERO)
                    .build();
        }
    }

    public FinancialReport getReportByDate(LocalDate date) {
        return financialReportRepository.findByReportDate(date)
                .orElseThrow(() -> new RuntimeException("Report not found for date: " + date));
    }

    public List<FinancialReport> getChartData(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        return financialReportRepository.findByReportDateBetweenOrderByReportDateAsc(startDate, endDate);
    }

    public List<FinancialReport> getChartDataCustom(LocalDate startDate, LocalDate endDate) {
        return financialReportRepository.findByReportDateBetweenOrderByReportDateAsc(startDate, endDate);
    }
}