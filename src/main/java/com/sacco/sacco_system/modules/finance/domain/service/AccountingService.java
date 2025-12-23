package com.sacco.sacco_system.modules.finance.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sacco.sacco_system.modules.admin.domain.entity.Asset;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.AccountType;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GLAccount;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GlMapping;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.JournalEntry;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.JournalLine;
import com.sacco.sacco_system.modules.finance.domain.repository.GLAccountRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.GlMappingRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.JournalEntryRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.JournalLineRepository;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.savings.domain.entity.Withdrawal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Full AccountingService with double-entry bookkeeping
 * Handles GL posting, fiscal periods, and journal entries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountingService {

    private final GLAccountRepository glAccountRepository;
    private final GlMappingRepository glMappingRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final ObjectMapper objectMapper;

    /**
     * DTO for manual journal entry posting
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualEntryRequest {
        private String description;
        private String reference;
        private LocalDate date;
        private List<ManualEntryLine> lines;
    }

    /**
     * DTO for journal line item
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualEntryLine {
        private String accountCode;
        private BigDecimal debit = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;
    }

    /**
     * Dynamic posting - looks up account codes from GL mapping configuration
     */
    @Transactional
    public void postEvent(String eventName, String description, String referenceNo, BigDecimal amount, String overrideDebitAccount) {
        GlMapping mapping = glMappingRepository.findByEventName(eventName)
                .orElseThrow(() -> new RuntimeException("GL Mapping not found for event: " + eventName));

        // Use the override if provided, otherwise fall back to the default configured in accounts.json
        String debitCode = (overrideDebitAccount != null && !overrideDebitAccount.isEmpty()) 
                ? overrideDebitAccount 
                : mapping.getDebitAccountCode();

        postDoubleEntry(description, referenceNo, debitCode, mapping.getCreditAccountCode(), amount);
    }

    @Transactional
    public void postEvent(String eventName, String description, String referenceNo, BigDecimal amount) {
        postEvent(eventName, description, referenceNo, amount, null);
    }

    /**
     * Core double-entry posting method
     */
    @Transactional
    public void postDoubleEntry(String description, String referenceNo, String debitAccountCode, String creditAccountCode, BigDecimal amount) {
        GLAccount debitAcct = glAccountRepository.findById(debitAccountCode)
                .orElseThrow(() -> new RuntimeException("Debit Account not found: " + debitAccountCode));

        GLAccount creditAcct = glAccountRepository.findById(creditAccountCode)
                .orElseThrow(() -> new RuntimeException("Credit Account not found: " + creditAccountCode));

        JournalEntry entry = JournalEntry.builder()
                .transactionDate(LocalDateTime.now())
                .description(description)
                .referenceNo(referenceNo)
                .build();

        JournalLine debitLine = JournalLine.builder()
                .journalEntry(entry)
                .account(debitAcct)
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .build();

        JournalLine creditLine = JournalLine.builder()
                .journalEntry(entry)
                .account(creditAcct)
                .debit(BigDecimal.ZERO)
                .credit(amount)
                .build();

        entry.setLines(List.of(debitLine, creditLine));

        updateBalance(debitAcct, amount, true);
        updateBalance(creditAcct, amount, false);

        journalEntryRepository.save(entry);
        log.debug("Posted journal entry: {} - DR: {} CR: {} Amount: {}", description, debitAccountCode, creditAccountCode, amount);
    }

    /**
     * Post manual journal entry with multiple lines
     */
    @Transactional
    public void postManualJournalEntry(ManualEntryRequest request) {
        BigDecimal totalDebit = request.getLines().stream()
                .map(ManualEntryLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = request.getLines().stream()
                .map(ManualEntryLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new RuntimeException("Journal Entry is unbalanced! Total Debit: " + totalDebit + ", Total Credit: " + totalCredit);
        }

        JournalEntry entry = JournalEntry.builder()
                .transactionDate(request.getDate().atStartOfDay())
                .postedDate(LocalDateTime.now())
                .description(request.getDescription())
                .referenceNo(request.getReference())
                .lines(new ArrayList<>())
                .build();

        for (ManualEntryLine lineDto : request.getLines()) {
            GLAccount account = glAccountRepository.findById(lineDto.getAccountCode())
                    .orElseThrow(() -> new RuntimeException("Account not found: " + lineDto.getAccountCode()));

            JournalLine line = JournalLine.builder()
                    .journalEntry(entry)
                    .account(account)
                    .debit(lineDto.getDebit())
                    .credit(lineDto.getCredit())
                    .build();
            entry.getLines().add(line);

            if (lineDto.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                updateBalance(account, lineDto.getDebit(), true);
            } else {
                updateBalance(account, lineDto.getCredit(), false);
            }
        }
        journalEntryRepository.save(entry);
        log.info("Posted manual journal entry: {}", request.getDescription());
    }

    /**
     * Update GL account balance based on account type and debit/credit
     */
    private void updateBalance(GLAccount account, BigDecimal amount, boolean isDebit) {
        if (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE) {
            account.setBalance(isDebit ? account.getBalance().add(amount) : account.getBalance().subtract(amount));
        } else {
            account.setBalance(isDebit ? account.getBalance().subtract(amount) : account.getBalance().add(amount));
        }
        glAccountRepository.save(account);
    }

    /**
     * Get account balance
     */
    public BigDecimal getAccountBalance(String glCode) {
        GLAccount account = glAccountRepository.findById(glCode)
                .orElseThrow(() -> new RuntimeException("GL Account not found: " + glCode));
        return account.getBalance();
    }

    /**
     * Toggle account active status
     */
    @Transactional
    public GLAccount toggleAccountStatus(String code) {
        GLAccount account = glAccountRepository.findById(code)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setActive(!account.isActive());
        return glAccountRepository.save(account);
    }

    /**
     * Create manual GL account
     */
    @Transactional
    public GLAccount createManualAccount(GLAccount account) {
        if (glAccountRepository.existsById(account.getCode())) {
            throw new RuntimeException("Account code already exists.");
        }
        account.setBalance(BigDecimal.ZERO);
        account.setActive(true);
        return glAccountRepository.save(account);
    }

    /**
     * Get accounts with balances as of date range
     */
    public List<GLAccount> getAccountsWithBalancesAsOf(LocalDate startDate, LocalDate endDate) {
        List<GLAccount> allAccounts = glAccountRepository.findAll();
        List<Object[]> totals = (startDate == null)
                ? journalLineRepository.getAccountTotalsUpToDate(endDate.atTime(LocalTime.MAX))
                : journalLineRepository.getAccountTotalsInRange(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        for (GLAccount account : allAccounts) {
            BigDecimal debitSum = BigDecimal.ZERO;
            BigDecimal creditSum = BigDecimal.ZERO;

            for (Object[] row : totals) {
                if (row[0].equals(account.getCode())) {
                    debitSum = (row[1] != null) ? (BigDecimal) row[1] : BigDecimal.ZERO;
                    creditSum = (row[2] != null) ? (BigDecimal) row[2] : BigDecimal.ZERO;
                    break;
                }
            }
            BigDecimal netBalance = (account.getType() == AccountType.ASSET || account.getType() == AccountType.EXPENSE)
                    ? debitSum.subtract(creditSum) : creditSum.subtract(debitSum);
            account.setBalance(netBalance);
        }
        return allAccounts;
    }

    /**
     * Get journal entries within date range
     */
    public List<JournalEntry> getJournalEntries(LocalDate startDate, LocalDate endDate) {
        return journalEntryRepository.findByTransactionDateBetween(
            startDate.atStartOfDay(),
            endDate.atTime(LocalTime.MAX)
        );
    }

    /**
     * Post asset purchase transaction
     */
    public void postAssetPurchase(Asset asset, String description) {
        log.info("Posting asset purchase for asset {}", asset.getId());
        postEvent("ASSET_PURCHASE", description, "ASSET-" + asset.getId(), asset.getPurchaseCost());
    }

    /**
     * Post loan disbursement transaction
     */
    public void postLoanDisbursement(Loan loan) {
        log.info("Posting loan disbursement for loan {}", loan.getLoanNumber());
        postEvent("LOAN_DISBURSEMENT", "Loan Disbursement - " + loan.getLoanNumber(),
                loan.getLoanNumber(), loan.getPrincipalAmount());
    }

    /**
     * Post loan repayment transaction (principal and interest)
     */
    public void postLoanRepayment(Loan loan, BigDecimal principalAmount, BigDecimal interestAmount) {
        log.info("Posting loan repayment for loan {} - Principal: {}, Interest: {}",
                loan.getLoanNumber(), principalAmount, interestAmount);

        if (principalAmount.compareTo(BigDecimal.ZERO) > 0) {
            postEvent("LOAN_REPAYMENT_PRINCIPAL", "Loan Principal Repayment - " + loan.getLoanNumber(),
                    loan.getLoanNumber(), principalAmount);
        }

        if (interestAmount.compareTo(BigDecimal.ZERO) > 0) {
            postEvent("LOAN_REPAYMENT_INTEREST", "Loan Interest Payment - " + loan.getLoanNumber(),
                    loan.getLoanNumber(), interestAmount);
        }
    }

    /**
     * Post savings deposit transaction
     */
    public void postSavingsDeposit(Member member, BigDecimal amount) {
        log.info("Posting savings deposit for member {} amount: {}", member.getMemberNumber(), amount);
        postEvent("SAVINGS_DEPOSIT", "Savings Deposit - " + member.getMemberNumber(),
                "DEP-" + member.getMemberNumber(), amount);
    }

    /**
     * Post savings withdrawal transaction
     */
    public void postSavingsWithdrawal(Withdrawal withdrawal) {
        log.info("Posting savings withdrawal {}", withdrawal.getId());
        postEvent("SAVINGS_WITHDRAWAL", "Savings Withdrawal",
                "WD-" + withdrawal.getId(), withdrawal.getAmount());
    }

    /**
     * Post member fee transaction
     */
    public void postMemberFee(Member member, BigDecimal amount, String feeType) {
        log.info("Posting {} fee for member {} amount: {}", feeType, member.getMemberNumber(), amount);
        String eventName = feeType.toUpperCase().replace(" ", "_") + "_FEE";
        postEvent(eventName, feeType + " - " + member.getMemberNumber(),
                "FEE-" + member.getMemberNumber(), amount);
    }

    /**
     * Post share capital purchase
     */
    public void postShareCapitalPurchase(Member member, BigDecimal amount) {
        log.info("Posting share capital purchase for member {} amount: {}", member.getMemberNumber(), amount);
        postEvent("SHARE_CAPITAL_PURCHASE", "Share Capital Purchase - " + member.getMemberNumber(),
                "SHARE-" + member.getMemberNumber(), amount);
    }

    /**
     * Post dividend payment
     */
    public void postDividendPayment(Member member, BigDecimal amount) {
        log.info("Posting dividend payment for member {} amount: {}", member.getMemberNumber(), amount);
        postEvent("DIVIDEND_PAYMENT", "Dividend Payment - " + member.getMemberNumber(),
                "DIV-" + member.getMemberNumber(), amount);
    }

    /**
     * Post fine/penalty payment
     */
    public void postFinePayment(Member member, BigDecimal amount) {
        log.info("Posting fine payment for member {} amount: {}", member.getMemberNumber(), amount);
        postEvent("FINE_PAYMENT", "Fine/Penalty Payment - " + member.getMemberNumber(),
                "FINE-" + member.getMemberNumber(), amount);
    }

    /**
     * Initialize Chart of Accounts from accounts.json
     */
    public void initChartOfAccounts() {
        long accountCount = glAccountRepository.count();

        if (accountCount > 0) {
            log.info("ℹ️ GL Accounts already initialized ({} accounts), skipping...", accountCount);
            return;
        }

        try {
            log.info("📊 Loading Chart of Accounts from accounts.json...");

            ClassPathResource resource = new ClassPathResource("accounts.json");
            InputStream inputStream = resource.getInputStream();

            // Use the injected ObjectMapper instead of creating a new one
            List<Map<String, String>> accountsData = objectMapper.readValue(
                inputStream,
                new TypeReference<List<Map<String, String>>>() {}
            );

            for (Map<String, String> accountData : accountsData) {
                GLAccount account = GLAccount.builder()
                        .code(accountData.get("code"))
                        .name(accountData.get("name"))
                        .type(AccountType.valueOf(accountData.get("type")))
                        .balance(BigDecimal.ZERO)
                        .active(true)
                        .build();

                glAccountRepository.save(account);
            }

            log.info("✅ Initialized {} GL Accounts from accounts.json", accountsData.size());
        } catch (Exception e) {
            log.error("❌ Failed to initialize GL Accounts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Chart of Accounts", e);
        }
    }

    /**
     * Initialize default GL mappings for common transaction types
     */
    public void initDefaultMappings() {
        long mappingCount = glMappingRepository.count();

        if (mappingCount > 0) {
            log.info("ℹ️ GL Mappings already initialized ({} mappings), skipping...", mappingCount);
            return;
        }

        log.info("🔗 Creating default GL Mappings...");

        // Savings Deposit Mapping
        createMapping("SAVINGS_DEPOSIT", "1002", "2001", "Member Savings Deposit");

        // Loan Disbursement Mapping
        createMapping("LOAN_DISBURSEMENT", "1200", "1002", "Loan Disbursement");

        // Loan Repayment Mapping (Principal)
        createMapping("LOAN_REPAYMENT_PRINCIPAL", "1002", "1200", "Loan Principal Repayment");

        // Loan Repayment Mapping (Interest)
        createMapping("LOAN_REPAYMENT_INTEREST", "1002", "4002", "Loan Interest Income");

        // Registration Fee Mapping
        createMapping("REGISTRATION_FEE", "1002", "4001", "Member Registration Fee");

        // Loan Processing Fee Mapping
        createMapping("LOAN_PROCESSING_FEE", "1002", "4005", "Loan Processing Fee");

        // Share Capital Purchase Mapping
        createMapping("SHARE_CAPITAL_PURCHASE", "1002", "3001", "Share Capital Purchase");

        // Dividend Payment Mapping
        createMapping("DIVIDEND_PAYMENT", "2003", "1002", "Dividend Payment");

        // Fine/Penalty Payment Mapping
        createMapping("FINE_PAYMENT", "1002", "4004", "Fine/Penalty Payment");

        log.info("✅ Created default GL Mappings");
    }

    private void createMapping(String eventType, String debitAccount, String creditAccount, String description) {
        GlMapping mapping = GlMapping.builder()
                .eventName(eventType)
                .debitAccountCode(debitAccount)
                .creditAccountCode(creditAccount)
                .descriptionTemplate(description)
                .build();

        glMappingRepository.save(mapping);
        log.debug("Created mapping: {} -> DR: {} CR: {}", eventType, debitAccount, creditAccount);
    }
}





