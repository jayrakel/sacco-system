package com.sacco.sacco_system.service;

import com.sacco.sacco_system.entity.FinancialReport;
import com.sacco.sacco_system.entity.Transaction; // Import this
import com.sacco.sacco_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private final TransactionRepository transactionRepository; // ✅ Inject this

    public FinancialReport generateDailyReport() {
        LocalDate today = LocalDate.now();

        // Fetch Income Streams
        BigDecimal totalInterest = loanRepository.getTotalInterest() != null
                ? loanRepository.getTotalInterest() : BigDecimal.ZERO;

        // ✅ NEW: Fetch Registration Fees
        BigDecimal totalRegFees = transactionRepository.getTotalAmountByType(Transaction.TransactionType.REGISTRATION_FEE);
        if (totalRegFees == null) totalRegFees = BigDecimal.ZERO;

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
                .totalWithdrawals(withdrawalRepository.getTotalWithdrawals() != null
                        ? withdrawalRepository.getTotalWithdrawals() : BigDecimal.ZERO)
                .build();

        // ✅ CORRECT CALCULATION: Net Income = Interest + Reg Fees - Withdrawals
        BigDecimal income = totalInterest.add(totalRegFees);
        BigDecimal expenses = report.getTotalWithdrawals();

        report.setNetIncome(income.subtract(expenses));

        return financialReportRepository.save(report);
    }

    public FinancialReport getTodayReport() {
        LocalDate today = LocalDate.now();
        // Always generate fresh report on request to show latest fees
        return generateDailyReport();
    }

    public FinancialReport getReportByDate(LocalDate date) {
        return financialReportRepository.findByReportDate(date)
                .orElseThrow(() -> new RuntimeException("Report not found for date: " + date));
    }
}