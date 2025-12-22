package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.*;
import com.sacco.sacco_system.modules.finance.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChartOfAccountsSetupService {

    private final GLAccountRepository accountRepository;
    private final GlMappingRepository mappingRepository;

    /**
     * Initialize Chart of Accounts with default accounts
     * This should be called once during system setup
     */
    @Transactional
    public void initializeChartOfAccounts() {
        log.info("Initializing Chart of Accounts...");

        List<GLAccount> accounts = new ArrayList<>();

        // === ASSETS (1000-1999) ===
        accounts.add(createAccount("1010", "Cash on Hand", AccountType.ASSET, true));
        accounts.add(createAccount("1020", "Bank Account - Main", AccountType.ASSET, true));
        accounts.add(createAccount("1030", "Bank Account - Savings", AccountType.ASSET, true));
        accounts.add(createAccount("1100", "Loans Receivable", AccountType.ASSET, true));
        accounts.add(createAccount("1110", "Loans Receivable - Short Term", AccountType.ASSET, true));
        accounts.add(createAccount("1120", "Loans Receivable - Long Term", AccountType.ASSET, true));
        accounts.add(createAccount("1200", "Accrued Interest Receivable", AccountType.ASSET, true));
        accounts.add(createAccount("1300", "Fixed Assets", AccountType.ASSET, true));
        accounts.add(createAccount("1310", "Office Equipment", AccountType.ASSET, true));
        accounts.add(createAccount("1320", "Furniture & Fixtures", AccountType.ASSET, true));

        // === LIABILITIES (2000-2999) ===
        accounts.add(createAccount("2010", "Member Savings Deposits", AccountType.LIABILITY, true));
        accounts.add(createAccount("2020", "Share Capital", AccountType.LIABILITY, true));
        accounts.add(createAccount("2100", "Accrued Interest Payable", AccountType.LIABILITY, true));
        accounts.add(createAccount("2200", "Accounts Payable", AccountType.LIABILITY, true));
        accounts.add(createAccount("2300", "Statutory Reserves", AccountType.LIABILITY, true));

        // === EQUITY (3000-3999) ===
        accounts.add(createAccount("3010", "Retained Earnings", AccountType.EQUITY, true));
        accounts.add(createAccount("3020", "Current Year Earnings", AccountType.EQUITY, true));

        // === INCOME (4000-4999) ===
        accounts.add(createAccount("4010", "Interest Income on Loans", AccountType.INCOME, true));
        accounts.add(createAccount("4020", "Fee Income - Application Fees", AccountType.INCOME, true));
        accounts.add(createAccount("4030", "Fee Income - Processing Fees", AccountType.INCOME, true));
        accounts.add(createAccount("4040", "Fine Income", AccountType.INCOME, true));
        accounts.add(createAccount("4050", "Miscellaneous Income", AccountType.INCOME, true));

        // === EXPENSES (5000-5999) ===
        accounts.add(createAccount("5010", "Operating Expenses", AccountType.EXPENSE, true));
        accounts.add(createAccount("5020", "Salaries & Wages", AccountType.EXPENSE, true));
        accounts.add(createAccount("5030", "Bank Charges", AccountType.EXPENSE, true));
        accounts.add(createAccount("5040", "Office Rent", AccountType.EXPENSE, true));
        accounts.add(createAccount("5050", "Utilities", AccountType.EXPENSE, true));
        accounts.add(createAccount("5060", "Depreciation", AccountType.EXPENSE, true));

        // Save all accounts
        accountRepository.saveAll(accounts);

        log.info("Chart of Accounts initialized with {} accounts", accounts.size());
    }

    /**
     * Create GL Mappings for all transaction types
     * Maps transaction types to their debit/credit accounts
     */
    @Transactional
    public void initializeGLMappings() {
        log.info("Initializing GL Mappings...");

        // Check existing mappings count
        long existingCount = mappingRepository.count();
        if (existingCount > 0) {
            log.info("GL Mappings already exist ({} mappings). Adding any missing mappings...", existingCount);
        }

        List<GlMapping> mappings = new ArrayList<>();

        // LOAN DISBURSEMENT
        // When loan is given: DEBIT Loans Receivable, CREDIT Bank Account
        mappings.add(createMapping("LOAN_DISBURSEMENT", "1100", "1020",
                "Loan disbursed to member"));

        // LOAN REPAYMENT - PRINCIPAL
        // When principal repaid: DEBIT Bank, CREDIT Loans Receivable
        mappings.add(createMapping("LOAN_REPAYMENT_PRINCIPAL", "1020", "1100",
                "Loan principal repayment"));

        // LOAN REPAYMENT - INTEREST
        // When interest repaid: DEBIT Bank, CREDIT Interest Income
        mappings.add(createMapping("LOAN_REPAYMENT_INTEREST", "1020", "4010",
                "Loan interest payment"));

        // SAVINGS DEPOSIT
        // When member deposits: DEBIT Bank, CREDIT Member Savings
        mappings.add(createMapping("SAVINGS_DEPOSIT", "1020", "2010",
                "Member savings deposit"));

        // SAVINGS WITHDRAWAL
        // When member withdraws: DEBIT Member Savings, CREDIT Bank
        mappings.add(createMapping("SAVINGS_WITHDRAWAL", "2010", "1020",
                "Member savings withdrawal"));

        // APPLICATION FEE
        // When fee paid: DEBIT Bank, CREDIT Fee Income
        mappings.add(createMapping("APPLICATION_FEE", "1020", "4020",
                "Loan application fee"));

        // PROCESSING FEE
        mappings.add(createMapping("PROCESSING_FEE", "1020", "4030",
                "Loan processing fee"));

        // SHARE PURCHASE
        // When member buys shares: DEBIT Bank, CREDIT Share Capital
        mappings.add(createMapping("SHARE_PURCHASE", "1020", "2020",
                "Member share capital contribution"));

        // FINE PAYMENT
        mappings.add(createMapping("FINE_PAYMENT", "1020", "4040",
                "Fine payment from member"));

        // CONTRIBUTION TO CUSTOM PRODUCTS (meat contribution, harambee, etc.)
        // DEBIT Bank, CREDIT Member Deposits (Non-Withdrawable)
        mappings.add(createMapping("CONTRIBUTION_RECEIVED", "1020", "2001",
                "Contribution to custom product"));

        // SHARE CAPITAL CONTRIBUTION
        // DEBIT Bank, CREDIT Share Capital
        mappings.add(createMapping("SHARE_CAPITAL_CONTRIBUTION", "1020", "2020",
                "Share capital contribution"));

        // OPERATING EXPENSE
        mappings.add(createMapping("OPERATING_EXPENSE", "5010", "1020",
                "Operating expense payment"));

        // SALARY PAYMENT
        mappings.add(createMapping("SALARY_PAYMENT", "5020", "1020",
                "Salary payment to staff"));

        // BANK CHARGES
        mappings.add(createMapping("BANK_CHARGE", "5030", "1020",
                "Bank charges and fees"));

        // Save only new mappings (check if each mapping exists by event name)
        int addedCount = 0;
        for (GlMapping mapping : mappings) {
            if (mappingRepository.findByEventName(mapping.getEventName()).isEmpty()) {
                mappingRepository.save(mapping);
                addedCount++;
            }
        }

        long totalCount = mappingRepository.count();
        log.info("GL Mappings initialized. Added {} new mappings. Total: {}", addedCount, totalCount);
    }

    /**
     * Check if Chart of Accounts is already initialized
     */
    public boolean isInitialized() {
        return accountRepository.count() > 0;
    }

    /**
     * Reset Chart of Accounts (for testing/development only)
     */
    @Transactional
    public void resetChartOfAccounts() {
        log.warn("RESETTING Chart of Accounts - This will delete all accounts and mappings!");
        mappingRepository.deleteAll();
        accountRepository.deleteAll();
        log.info("Chart of Accounts reset complete");
    }

    // Helper methods
    private GLAccount createAccount(String code, String name, AccountType type, boolean active) {
        return GLAccount.builder()
                .code(code)
                .name(name)
                .type(type)
                .active(active)
                .build();
    }

    private GlMapping createMapping(String eventName, String debitAccount,
                                    String creditAccount, String description) {
        return GlMapping.builder()
                .eventName(eventName)
                .debitAccountCode(debitAccount)
                .creditAccountCode(creditAccount)
                .descriptionTemplate(description)
                .build();
    }
}

