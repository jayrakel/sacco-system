package com.sacco.sacco_system.service;

import com.sacco.sacco_system.dto.SavingsAccountDTO;
import com.sacco.sacco_system.entity.*;
import com.sacco.sacco_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingsService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final SavingsProductRepository savingsProductRepository; // ✅ Required for Product Logic
    private final AccountingService accountingService;

    // ========================================================================
    // 1. ACCOUNT MANAGEMENT (OPENING & RETRIEVAL)
    // ========================================================================

    /**
     * OPEN NEW ACCOUNT (Linked to a Product)
     * Replaces the old 'createSavingsAccount' to enforce product rules.
     */
    public SavingsAccountDTO openAccount(UUID memberId, UUID productId, BigDecimal initialDeposit) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        SavingsProduct product = savingsProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Generate Number based on Type (e.g., FD001, SAV001)
        String prefix = product.getType() == SavingsProduct.ProductType.FIXED_DEPOSIT ? "FD" : "SAV";
        String accNumber = prefix + String.format("%06d", savingsAccountRepository.count() + 1);

        SavingsAccount account = SavingsAccount.builder()
                .member(member)
                .product(product)
                .accountNumber(accNumber)
                .balance(BigDecimal.ZERO)
                .totalDeposits(BigDecimal.ZERO)
                .totalWithdrawals(BigDecimal.ZERO)
                .status(SavingsAccount.AccountStatus.ACTIVE)
                .build();

        // Handle Fixed Deposit / Lock-in periods
        if (product.getMinDurationMonths() != null && product.getMinDurationMonths() > 0) {
            account.setMaturityDate(LocalDate.now().plusMonths(product.getMinDurationMonths()));
        }

        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        // Process Initial Deposit if provided
        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            deposit(savedAccount.getAccountNumber(), initialDeposit, "Opening Deposit");
        }

        return convertToDTO(savedAccount);
    }

    public SavingsAccountDTO getSavingsAccountById(UUID id) {
        return convertToDTO(savingsAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings account not found")));
    }

    public SavingsAccountDTO getSavingsAccountByNumber(String accountNumber) {
        return convertToDTO(savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Savings account not found")));
    }

    public List<SavingsAccountDTO> getSavingsAccountsByMemberId(UUID memberId) {
        return savingsAccountRepository.findByMemberId(memberId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<SavingsAccountDTO> getAllSavingsAccounts() {
        return savingsAccountRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BigDecimal getTotalSavingsBalance() {
        BigDecimal total = savingsAccountRepository.getTotalActiveAccountsBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    // ========================================================================
    // 2. TRANSACTION PROCESSING (DEPOSIT, WITHDRAW, TRANSFER)
    // ========================================================================

    public SavingsAccountDTO deposit(String accountNumber, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be positive");
        }

        SavingsAccount account = savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // 1. Update Account
        account.setBalance(account.getBalance().add(amount));
        account.setTotalDeposits(account.getTotalDeposits().add(amount));
        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        // 2. Update Member Totals (Main tracking)
        Member member = account.getMember();
        member.setTotalSavings(member.getTotalSavings().add(amount));
        memberRepository.save(member);

        // 3. Record Transaction
        Transaction tx = Transaction.builder()
                .savingsAccount(account)
                .member(member)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .description(description != null ? description : "Deposit")
                .balanceAfter(savedAccount.getBalance())
                .build();
        transactionRepository.save(tx);

        // 4. GL Posting (Debit Cash 1001, Credit Savings 2001)
        accountingService.postDoubleEntry("Deposit " + accountNumber, tx.getTransactionId(), "1001", "2001", amount);

        return convertToDTO(savedAccount);
    }

    public SavingsAccountDTO withdraw(String accountNumber, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Amount must be positive");

        SavingsAccount account = savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // ✅ VALIDATION 1: Locked Accounts (Welfare / Fixed Deposits)
        if (account.getProduct() != null && !account.getProduct().isAllowWithdrawal()) {
            // Allow withdrawal ONLY if matured (for FD)
            if (account.getMaturityDate() != null) {
                if (LocalDate.now().isBefore(account.getMaturityDate())) {
                    throw new RuntimeException("Account is locked until maturity: " + account.getMaturityDate());
                }
            } else {
                // For Welfare/Social funds with no maturity date, completely block
                throw new RuntimeException("Withdrawals not allowed for this account type.");
            }
        }

        // ✅ VALIDATION 2: Minimum Balance Rule
        BigDecimal minBalance = (account.getProduct() != null && account.getProduct().getMinBalance() != null)
                ? account.getProduct().getMinBalance() : BigDecimal.ZERO;

        if (account.getBalance().subtract(amount).compareTo(minBalance) < 0) {
            throw new RuntimeException("Withdrawal failed. Minimum balance of " + minBalance + " must be maintained.");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // Process Withdrawal
        account.setBalance(account.getBalance().subtract(amount));
        account.setTotalWithdrawals(account.getTotalWithdrawals().add(amount));
        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        Member member = account.getMember();
        member.setTotalSavings(member.getTotalSavings().subtract(amount));
        memberRepository.save(member);

        Transaction tx = Transaction.builder()
                .savingsAccount(account)
                .member(member)
                .type(Transaction.TransactionType.WITHDRAWAL)
                .amount(amount)
                .description(description != null ? description : "Withdrawal")
                .balanceAfter(savedAccount.getBalance())
                .build();
        transactionRepository.save(tx);

        // GL Posting (Debit Savings 2001, Credit Cash 1001)
        accountingService.postDoubleEntry("Withdrawal " + accountNumber, tx.getTransactionId(), "2001", "1001", amount);

        return convertToDTO(savedAccount);
    }

    public void transferFunds(String fromAccountNum, String toAccountNum, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Amount must be positive");
        if (fromAccountNum.equals(toAccountNum)) throw new RuntimeException("Cannot transfer to self");

        // Use existing methods to ensure validations (min balance, locks) run automatically
        withdraw(fromAccountNum, amount, "Transfer to " + toAccountNum + (description != null ? ": " + description : ""));
        deposit(toAccountNum, amount, "Transfer from " + fromAccountNum + (description != null ? ": " + description : ""));
    }

    // ========================================================================
    // 3. INTEREST CALCULATION
    // ========================================================================

    /**
     * BATCH INTEREST: Calculates based on EACH account's specific product rate.
     * No longer takes a global rate param.
     */
    public void applyMonthlyInterest() {
        List<SavingsAccount> accounts = savingsAccountRepository.findAll();

        for (SavingsAccount acc : accounts) {
            if (acc.getBalance().compareTo(BigDecimal.ZERO) > 0 && acc.getStatus() == SavingsAccount.AccountStatus.ACTIVE) {

                // Get Rate from Product (if null, skip)
                if (acc.getProduct() == null || acc.getProduct().getInterestRate() == null) continue;

                BigDecimal annualRate = acc.getProduct().getInterestRate();
                if (annualRate.compareTo(BigDecimal.ZERO) == 0) continue;

                // Monthly Rate = Annual / 1200
                BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 8, RoundingMode.HALF_UP);
                BigDecimal interest = acc.getBalance().multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

                if (interest.compareTo(BigDecimal.ZERO) > 0) {
                    acc.setBalance(acc.getBalance().add(interest));
                    // Track Accrued Interest if field exists, otherwise just balance
                    if(acc.getAccruedInterest() != null) {
                        acc.setAccruedInterest(acc.getAccruedInterest().add(interest));
                    }
                    savingsAccountRepository.save(acc);

                    Transaction tx = Transaction.builder()
                            .savingsAccount(acc)
                            .member(acc.getMember())
                            .type(Transaction.TransactionType.INTEREST_EARNED)
                            .amount(interest)
                            .description("Monthly Interest (" + annualRate + "%)")
                            .balanceAfter(acc.getBalance())
                            .build();
                    transactionRepository.save(tx);

                    // GL: Debit Interest Expense (5006), Credit Savings (2001)
                    try {
                        accountingService.postDoubleEntry("Interest " + acc.getAccountNumber(), tx.getTransactionId(), "5006", "2001", interest);
                    } catch (Exception e) {
                        System.err.println("GL Post Failed for Interest: " + e.getMessage());
                    }
                }
            }
        }
    }

    // ========================================================================
    // 4. HELPERS
    // ========================================================================

    private String generateAccountNumber() {
        long count = savingsAccountRepository.count();
        return "SAV" + String.format("%06d", count + 1);
    }


    private SavingsAccountDTO convertToDTO(SavingsAccount account) {
        String productName = (account.getProduct() != null) ? account.getProduct().getName() : "Ordinary Savings";
        BigDecimal rate = (account.getProduct() != null) ? account.getProduct().getInterestRate() : BigDecimal.ZERO;

        return SavingsAccountDTO.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .memberId(account.getMember().getId())
                .memberName(account.getMember().getFirstName() + " " + account.getMember().getLastName())
                .balance(account.getBalance())
                .totalDeposits(account.getTotalDeposits())
                .totalWithdrawals(account.getTotalWithdrawals())
                .status(account.getStatus().toString())
                // ✅ Populate New Fields
                .productName(productName)
                .interestRate(rate)
                .maturityDate(account.getMaturityDate())
                .build();
    }
}