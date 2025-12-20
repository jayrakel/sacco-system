package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.AccountType;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GLAccount;
import com.sacco.sacco_system.modules.finance.domain.repository.GLAccountRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.JournalEntryRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.JournalLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Accounting-Based Financial Report Service
 * Generates financial statements from GL accounts and journal entries
 * This proves that our accounting integration is working!
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingReportService {

    private final GLAccountRepository glAccountRepository;
    private final JournalLineRepository journalLineRepository;
    private final JournalEntryRepository journalEntryRepository;

    /**
     * Generate Balance Sheet from GL Account Balances
     * Assets = Liabilities + Equity
     */
    public Map<String, Object> getBalanceSheet(LocalDate asOfDate) {
        log.info("Generating Balance Sheet as of {}", asOfDate);

        List<GLAccount> allAccounts = glAccountRepository.findAll();

        // Group accounts by type
        Map<AccountType, List<GLAccount>> accountsByType = allAccounts.stream()
                .filter(GLAccount::isActive)
                .collect(Collectors.groupingBy(GLAccount::getType));

        // Calculate totals for each section
        BigDecimal totalAssets = calculateTotal(accountsByType.get(AccountType.ASSET));
        BigDecimal totalLiabilities = calculateTotal(accountsByType.get(AccountType.LIABILITY));
        BigDecimal totalEquity = calculateTotal(accountsByType.get(AccountType.EQUITY));

        // Build detailed breakdown
        Map<String, Object> balanceSheet = new HashMap<>();

        // Assets section
        Map<String, Object> assets = new HashMap<>();
        assets.put("accounts", buildAccountList(accountsByType.get(AccountType.ASSET)));
        assets.put("total", totalAssets);
        balanceSheet.put("assets", assets);

        // Liabilities section
        Map<String, Object> liabilities = new HashMap<>();
        liabilities.put("accounts", buildAccountList(accountsByType.get(AccountType.LIABILITY)));
        liabilities.put("total", totalLiabilities);
        balanceSheet.put("liabilities", liabilities);

        // Equity section
        Map<String, Object> equity = new HashMap<>();
        equity.put("accounts", buildAccountList(accountsByType.get(AccountType.EQUITY)));
        equity.put("total", totalEquity);
        balanceSheet.put("equity", equity);

        // Totals and balance check
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
        boolean balanced = totalAssets.compareTo(totalLiabilitiesAndEquity) == 0;

        balanceSheet.put("totalAssets", totalAssets);
        balanceSheet.put("totalLiabilitiesAndEquity", totalLiabilitiesAndEquity);
        balanceSheet.put("balanced", balanced);
        balanceSheet.put("asOfDate", asOfDate);

        if (!balanced) {
            BigDecimal difference = totalAssets.subtract(totalLiabilitiesAndEquity);
            balanceSheet.put("difference", difference);
            log.warn("Balance Sheet is OUT OF BALANCE by {}", difference);
        } else {
            log.info("Balance Sheet is BALANCED ✓");
        }

        return balanceSheet;
    }

    /**
     * Generate Income Statement from Journal Entries
     * Revenue - Expenses = Net Income
     */
    public Map<String, Object> getIncomeStatement(LocalDate startDate, LocalDate endDate) {
        log.info("Generating Income Statement from {} to {}", startDate, endDate);

        List<GLAccount> allAccounts = glAccountRepository.findAll();

        // Get income and expense accounts
        List<GLAccount> incomeAccounts = allAccounts.stream()
                .filter(GLAccount::isActive)
                .filter(a -> a.getType() == AccountType.INCOME)
                .collect(Collectors.toList());

        List<GLAccount> expenseAccounts = allAccounts.stream()
                .filter(GLAccount::isActive)
                .filter(a -> a.getType() == AccountType.EXPENSE)
                .collect(Collectors.toList());

        // Calculate totals
        BigDecimal totalRevenue = calculateTotal(incomeAccounts);
        BigDecimal totalExpenses = calculateTotal(expenseAccounts);
        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);

        // Build statement
        Map<String, Object> incomeStatement = new HashMap<>();

        // Revenue section
        Map<String, Object> revenue = new HashMap<>();
        revenue.put("accounts", buildAccountList(incomeAccounts));
        revenue.put("total", totalRevenue);
        incomeStatement.put("revenue", revenue);

        // Expenses section
        Map<String, Object> expenses = new HashMap<>();
        expenses.put("accounts", buildAccountList(expenseAccounts));
        expenses.put("total", totalExpenses);
        incomeStatement.put("expenses", expenses);

        // Net income
        incomeStatement.put("netIncome", netIncome);
        incomeStatement.put("startDate", startDate);
        incomeStatement.put("endDate", endDate);

        // Performance metrics
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profitMargin = netIncome.divide(totalRevenue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            incomeStatement.put("profitMarginPercent", profitMargin);
        }

        return incomeStatement;
    }

    /**
     * Generate Trial Balance - All Account Balances
     * Debits should equal Credits
     */
    public Map<String, Object> getTrialBalance(LocalDate asOfDate) {
        log.info("Generating Trial Balance as of {}", asOfDate);

        List<GLAccount> allAccounts = glAccountRepository.findAll();

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        List<Map<String, Object>> accountBalances = new ArrayList<>();

        for (GLAccount account : allAccounts) {
            if (!account.isActive()) continue;

            BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;

            Map<String, Object> accountInfo = new HashMap<>();
            accountInfo.put("code", account.getCode());
            accountInfo.put("name", account.getName());
            accountInfo.put("type", account.getType().toString());
            accountInfo.put("balance", balance);

            // Determine debit/credit based on account type and balance
            if (isDebitAccount(account.getType())) {
                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    accountInfo.put("debit", balance);
                    accountInfo.put("credit", BigDecimal.ZERO);
                    totalDebits = totalDebits.add(balance);
                } else {
                    accountInfo.put("debit", BigDecimal.ZERO);
                    accountInfo.put("credit", balance.negate());
                    totalCredits = totalCredits.add(balance.negate());
                }
            } else {
                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    accountInfo.put("debit", BigDecimal.ZERO);
                    accountInfo.put("credit", balance);
                    totalCredits = totalCredits.add(balance);
                } else {
                    accountInfo.put("debit", balance.negate());
                    accountInfo.put("credit", BigDecimal.ZERO);
                    totalDebits = totalDebits.add(balance.negate());
                }
            }

            accountBalances.add(accountInfo);
        }

        // Sort by account code
        accountBalances.sort(Comparator.comparing(a -> (String) a.get("code")));

        Map<String, Object> trialBalance = new HashMap<>();
        trialBalance.put("accounts", accountBalances);
        trialBalance.put("totalDebits", totalDebits);
        trialBalance.put("totalCredits", totalCredits);
        trialBalance.put("balanced", totalDebits.compareTo(totalCredits) == 0);
        trialBalance.put("asOfDate", asOfDate);

        if (totalDebits.compareTo(totalCredits) != 0) {
            BigDecimal difference = totalDebits.subtract(totalCredits);
            trialBalance.put("difference", difference);
            log.warn("Trial Balance is OUT OF BALANCE by {}", difference);
        } else {
            log.info("Trial Balance is BALANCED ✓");
        }

        return trialBalance;
    }

    /**
     * Get Account Activity Summary
     * Shows transaction volume per account
     */
    public Map<String, Object> getAccountActivity(LocalDate startDate, LocalDate endDate) {
        log.info("Generating Account Activity from {} to {}", startDate, endDate);

        List<GLAccount> allAccounts = glAccountRepository.findAll();
        List<Map<String, Object>> activityList = new ArrayList<>();

        for (GLAccount account : allAccounts) {
            if (!account.isActive()) continue;

            // Get transaction count for this account
            Long transactionCount = journalLineRepository.countByAccountCodeAndDateRange(
                    account.getCode(), startDate, endDate);

            BigDecimal totalDebits = journalLineRepository.sumDebitsByAccountAndDateRange(
                    account.getCode(), startDate, endDate);
            BigDecimal totalCredits = journalLineRepository.sumCreditsByAccountAndDateRange(
                    account.getCode(), startDate, endDate);

            if (transactionCount > 0) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("accountCode", account.getCode());
                activity.put("accountName", account.getName());
                activity.put("transactionCount", transactionCount);
                activity.put("totalDebits", totalDebits != null ? totalDebits : BigDecimal.ZERO);
                activity.put("totalCredits", totalCredits != null ? totalCredits : BigDecimal.ZERO);
                activity.put("netChange",
                        (totalDebits != null ? totalDebits : BigDecimal.ZERO)
                        .subtract(totalCredits != null ? totalCredits : BigDecimal.ZERO));
                activityList.add(activity);
            }
        }

        // Sort by transaction count descending
        activityList.sort((a, b) ->
                Long.compare((Long) b.get("transactionCount"), (Long) a.get("transactionCount")));

        Map<String, Object> result = new HashMap<>();
        result.put("activity", activityList);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("totalAccounts", activityList.size());

        return result;
    }

    /**
     * Get Cash Flow Summary
     * Simplified cash flow showing cash inflows and outflows
     */
    public Map<String, Object> getCashFlowSummary(LocalDate startDate, LocalDate endDate) {
        log.info("Generating Cash Flow Summary from {} to {}", startDate, endDate);

        String cashAccountCode = "1020"; // Cash account

        BigDecimal cashInflows = journalLineRepository.sumDebitsByAccountAndDateRange(
                cashAccountCode, startDate, endDate);
        BigDecimal cashOutflows = journalLineRepository.sumCreditsByAccountAndDateRange(
                cashAccountCode, startDate, endDate);

        BigDecimal netCashFlow = (cashInflows != null ? cashInflows : BigDecimal.ZERO)
                .subtract(cashOutflows != null ? cashOutflows : BigDecimal.ZERO);

        // Get opening and closing balances
        GLAccount cashAccount = glAccountRepository.findByCode(cashAccountCode)
                .orElseThrow(() -> new RuntimeException("Cash account not found"));

        Map<String, Object> cashFlow = new HashMap<>();
        cashFlow.put("cashInflows", cashInflows != null ? cashInflows : BigDecimal.ZERO);
        cashFlow.put("cashOutflows", cashOutflows != null ? cashOutflows : BigDecimal.ZERO);
        cashFlow.put("netCashFlow", netCashFlow);
        cashFlow.put("currentCashBalance", cashAccount.getBalance());
        cashFlow.put("startDate", startDate);
        cashFlow.put("endDate", endDate);

        return cashFlow;
    }

    // Helper methods

    private BigDecimal calculateTotal(List<GLAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return accounts.stream()
                .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> buildAccountList(List<GLAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyList();
        }

        return accounts.stream()
                .sorted(Comparator.comparing(GLAccount::getCode))
                .map(account -> {
                    Map<String, Object> accountInfo = new HashMap<>();
                    accountInfo.put("code", account.getCode());
                    accountInfo.put("name", account.getName());
                    accountInfo.put("balance", account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO);
                    return accountInfo;
                })
                .collect(Collectors.toList());
    }

    private boolean isDebitAccount(AccountType type) {
        return type == AccountType.ASSET || type == AccountType.EXPENSE;
    }
}

