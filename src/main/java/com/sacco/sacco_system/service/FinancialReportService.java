package com.sacco.sacco_system.service;

import com.sacco.sacco_system.entity.FinancialReport;
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
    
    public FinancialReport generateDailyReport() {
        LocalDate today = LocalDate.now();
        
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
                .totalInterestCollected(loanRepository.getTotalInterest() != null 
                    ? loanRepository.getTotalInterest() : BigDecimal.ZERO)
                .totalShareCapital(shareCapitalRepository.getTotalShareCapital() != null 
                    ? shareCapitalRepository.getTotalShareCapital() : BigDecimal.ZERO)
                .totalWithdrawals(withdrawalRepository.getTotalWithdrawals() != null 
                    ? withdrawalRepository.getTotalWithdrawals() : BigDecimal.ZERO)
                .build();
        
        // Calculate net income
        BigDecimal income = report.getTotalInterestCollected();
        BigDecimal expenses = report.getTotalWithdrawals();
        report.setNetIncome(income.subtract(expenses));
        
        return financialReportRepository.save(report);
    }
    
    public FinancialReport getTodayReport() {
        LocalDate today = LocalDate.now();
        return financialReportRepository.findByReportDate(today)
                .orElseGet(this::generateDailyReport);
    }
    
    public FinancialReport getReportByDate(LocalDate date) {
        return financialReportRepository.findByReportDate(date)
                .orElseThrow(() -> new RuntimeException("Report not found for date: " + date));
    }
}
