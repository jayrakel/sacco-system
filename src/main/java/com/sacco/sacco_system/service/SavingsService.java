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
    private final SavingsProductRepository savingsProductRepository;
    private final AccountingService accountingService;

    // ========================================================================
    // 1. ACCOUNT MANAGEMENT
    // ========================================================================

    public SavingsAccountDTO openAccount(UUID memberId, UUID productId, BigDecimal initialDeposit) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        SavingsProduct product = savingsProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

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

        if (product.getMinDurationMonths() != null && product.getMinDurationMonths() > 0) {
            account.setMaturityDate(LocalDate.now().plusMonths(product.getMinDurationMonths()));
        }

        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            deposit(savedAccount.getAccountNumber(), initialDeposit, "Opening Deposit");
        }

        return convertToDTO(savedAccount);
    }

    public SavingsAccountDTO getSavingsAccountById(UUID id) {
        return convertToDTO(savingsAccountRepository.findById(id).orElseThrow(() -> new RuntimeException("Account not found")));
    }

    public SavingsAccountDTO getSavingsAccountByNumber(String accountNumber) {
        return convertToDTO(savingsAccountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found")));
    }

    // ✅ Matches Controller 'getMyBalance' call
    public List<SavingsAccountDTO> getMemberAccounts(UUID memberId) {
        return savingsAccountRepository.findByMemberId(memberId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ✅ Alias for Controller 'getSavingsAccountsByMemberId' call
    public List<SavingsAccountDTO> getSavingsAccountsByMemberId(UUID memberId) {
        return getMemberAccounts(memberId);
    }

    public List<SavingsAccountDTO> getAllSavingsAccounts() {
        return savingsAccountRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public BigDecimal getTotalSavingsBalance() {
        BigDecimal total = savingsAccountRepository.getTotalActiveAccountsBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    // ========================================================================
    // 2. TRANSACTIONS
    // ========================================================================

    public SavingsAccountDTO deposit(String accountNumber, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Amount must be positive");

        SavingsAccount account = savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setBalance(account.getBalance().add(amount));
        account.setTotalDeposits(account.getTotalDeposits().add(amount));
        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        Member member = account.getMember();
        member.setTotalSavings(member.getTotalSavings().add(amount));
        memberRepository.save(member);

        Transaction tx = Transaction.builder()
                .savingsAccount(account)
                .member(member)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .description(description != null ? description : "Deposit")
                .balanceAfter(savedAccount.getBalance())
                .build();
        transactionRepository.save(tx);

        accountingService.postDoubleEntry("Deposit " + accountNumber, tx.getTransactionId(), "1001", "2001", amount);
        return convertToDTO(savedAccount);
    }

    public SavingsAccountDTO withdraw(String accountNumber, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Amount must be positive");

        SavingsAccount account = savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getProduct() != null && !account.getProduct().isAllowWithdrawal()) {
            if (account.getMaturityDate() != null && LocalDate.now().isBefore(account.getMaturityDate())) {
                throw new RuntimeException("Account locked until: " + account.getMaturityDate());
            }
        }

        BigDecimal minBalance = (account.getProduct() != null && account.getProduct().getMinBalance() != null)
                ? account.getProduct().getMinBalance() : BigDecimal.ZERO;

        if (account.getBalance().subtract(amount).compareTo(minBalance) < 0) {
            throw new RuntimeException("Min balance of " + minBalance + " required.");
        }

        if (account.getBalance().compareTo(amount) < 0) throw new RuntimeException("Insufficient funds");

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

        accountingService.postDoubleEntry("Withdrawal " + accountNumber, tx.getTransactionId(), "2001", "1001", amount);
        return convertToDTO(savedAccount);
    }

    // ✅ RESTORED: Transfer Funds Method
    public void transferFunds(String fromAccountNum, String toAccountNum, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Amount must be positive");
        if (fromAccountNum.equals(toAccountNum)) throw new RuntimeException("Cannot transfer to self");

        // Atomic transfer: Withdraw from source, Deposit to destination
        withdraw(fromAccountNum, amount, "Transfer to " + toAccountNum + (description != null ? ": " + description : ""));
        deposit(toAccountNum, amount, "Transfer from " + fromAccountNum + (description != null ? ": " + description : ""));
    }

    // ========================================================================
    // 3. INTEREST CALCULATION
    // ========================================================================

    public void applyMonthlyInterest() {
        List<SavingsAccount> accounts = savingsAccountRepository.findAll();
        for (SavingsAccount acc : accounts) {
            if (acc.getBalance().compareTo(BigDecimal.ZERO) > 0 && acc.getStatus() == SavingsAccount.AccountStatus.ACTIVE) {
                if (acc.getProduct() == null || acc.getProduct().getInterestRate() == null) continue;

                BigDecimal annualRate = acc.getProduct().getInterestRate();
                BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 8, RoundingMode.HALF_UP);
                BigDecimal interest = acc.getBalance().multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

                if (interest.compareTo(BigDecimal.ZERO) > 0) {
                    acc.setBalance(acc.getBalance().add(interest));
                    if(acc.getAccruedInterest() != null) acc.setAccruedInterest(acc.getAccruedInterest().add(interest));
                    savingsAccountRepository.save(acc);

                    Transaction tx = Transaction.builder()
                            .savingsAccount(acc)
                            .member(acc.getMember())
                            .type(Transaction.TransactionType.INTEREST_EARNED)
                            .amount(interest)
                            .description("Monthly Interest")
                            .balanceAfter(acc.getBalance())
                            .build();
                    transactionRepository.save(tx);

                    try {
                        accountingService.postDoubleEntry("Interest " + acc.getAccountNumber(), tx.getTransactionId(), "5006", "2001", interest);
                    } catch (Exception e) {}
                }
            }
        }
    }

    // ========================================================================
    // 4. HELPERS
    // ========================================================================

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
                .productName(productName)
                .interestRate(rate)
                .maturityDate(account.getMaturityDate())
                .build();
    }
}