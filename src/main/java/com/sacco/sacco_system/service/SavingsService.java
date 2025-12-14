package com.sacco.sacco_system.service;

import com.sacco.sacco_system.dto.SavingsAccountDTO;
import com.sacco.sacco_system.entity.Member;
import com.sacco.sacco_system.entity.SavingsAccount;
import com.sacco.sacco_system.entity.Transaction;
import com.sacco.sacco_system.repository.MemberRepository;
import com.sacco.sacco_system.repository.SavingsAccountRepository;
import com.sacco.sacco_system.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final AccountingService accountingService;

    // --- EXISTING METHODS ---

    public SavingsAccountDTO createSavingsAccount(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        SavingsAccount account = SavingsAccount.builder()
                .member(member)
                .accountNumber(generateAccountNumber())
                .balance(BigDecimal.ZERO)
                .totalDeposits(BigDecimal.ZERO)
                .totalWithdrawals(BigDecimal.ZERO)
                .status(SavingsAccount.AccountStatus.ACTIVE)
                .build();

        SavingsAccount savedAccount = savingsAccountRepository.save(account);
        return convertToDTO(savedAccount);
    }

    public SavingsAccountDTO getSavingsAccountById(UUID id) {
        SavingsAccount account = savingsAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings account not found"));
        return convertToDTO(account);
    }

    public SavingsAccountDTO getSavingsAccountByNumber(String accountNumber) {
        SavingsAccount account = savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Savings account not found"));
        return convertToDTO(account);
    }

    public List<SavingsAccountDTO> getSavingsAccountsByMemberId(UUID memberId) {
        return savingsAccountRepository.findByMemberId(memberId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<SavingsAccountDTO> getAllSavingsAccounts() {
        return savingsAccountRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public SavingsAccountDTO deposit(String accountNumber, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be positive");
        }

        SavingsAccount account = savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Savings account not found"));

        // 1. Update Account
        account.setBalance(account.getBalance().add(amount));
        account.setTotalDeposits(account.getTotalDeposits().add(amount));
        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        // 2. Update Member Totals
        Member member = account.getMember();
        member.setTotalSavings(member.getTotalSavings().add(amount));
        memberRepository.save(member);

        // 3. Record Transaction
        Transaction transaction = Transaction.builder()
                .savingsAccount(account)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .description(description != null ? description : "Deposit")
                .balanceAfter(savedAccount.getBalance())
                .build();

        transactionRepository.save(transaction);

        // 4. POST TO GL (Double Entry)
        // Debit: Cash (1001) | Credit: Member Savings Liability (2001)
        accountingService.postDoubleEntry(
                "Deposit - " + account.getAccountNumber(),
                transaction.getTransactionId(),
                "1001",
                "2001",
                amount
        );

        return convertToDTO(savedAccount);
    }

    public SavingsAccountDTO withdraw(String accountNumber, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be positive");
        }

        SavingsAccount account = savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Savings account not found"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // 1. Update Account
        account.setBalance(account.getBalance().subtract(amount));
        account.setTotalWithdrawals(account.getTotalWithdrawals().add(amount));
        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        // 2. Update Member Totals
        Member member = account.getMember();
        member.setTotalSavings(member.getTotalSavings().subtract(amount));
        memberRepository.save(member);

        // 3. Record Transaction
        Transaction transaction = Transaction.builder()
                .savingsAccount(account)
                .type(Transaction.TransactionType.WITHDRAWAL)
                .amount(amount)
                .description(description != null ? description : "Withdrawal")
                .balanceAfter(savedAccount.getBalance())
                .build();

        transactionRepository.save(transaction);

        // 4. POST TO GL (Double Entry)
        // Debit: Member Savings Liability (2001) | Credit: Cash (1001)
        accountingService.postDoubleEntry(
                "Withdrawal - " + account.getAccountNumber(),
                transaction.getTransactionId(),
                "2001",
                "1001",
                amount
        );

        return convertToDTO(savedAccount);
    }

    // --- ✅ NEW METHODS: TRANSFER & INTEREST ---

    /**
     * INTERNAL TRANSFER (Member to Member)
     */
    public void transferFunds(String fromAccountNum, String toAccountNum, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Amount must be positive");
        if (fromAccountNum.equals(toAccountNum)) throw new RuntimeException("Cannot transfer to self");

        SavingsAccount fromAcc = savingsAccountRepository.findByAccountNumber(fromAccountNum)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));
        SavingsAccount toAcc = savingsAccountRepository.findByAccountNumber(toAccountNum)
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        if (fromAcc.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds for transfer");
        }

        // 1. Deduct from Sender
        fromAcc.setBalance(fromAcc.getBalance().subtract(amount));
        fromAcc.setTotalWithdrawals(fromAcc.getTotalWithdrawals().add(amount));
        savingsAccountRepository.save(fromAcc);

        // 2. Add to Receiver
        toAcc.setBalance(toAcc.getBalance().add(amount));
        toAcc.setTotalDeposits(toAcc.getTotalDeposits().add(amount));
        savingsAccountRepository.save(toAcc);

        // 3. Record Transaction (Sender Side)
        Transaction txSender = Transaction.builder()
                .member(fromAcc.getMember())
                .savingsAccount(fromAcc)
                .type(Transaction.TransactionType.TRANSFER)
                .paymentMethod(Transaction.PaymentMethod.SYSTEM)
                .amount(amount.negate()) // Negative for sender visualization
                .description("Transfer to " + toAcc.getMember().getFirstName() + ": " + description)
                .balanceAfter(fromAcc.getBalance())
                .build();
        transactionRepository.save(txSender);

        // 4. Record Transaction (Receiver Side)
        Transaction txReceiver = Transaction.builder()
                .member(toAcc.getMember())
                .savingsAccount(toAcc)
                .type(Transaction.TransactionType.TRANSFER)
                .paymentMethod(Transaction.PaymentMethod.SYSTEM)
                .amount(amount)
                .description("Transfer from " + fromAcc.getMember().getFirstName() + ": " + description)
                .balanceAfter(toAcc.getBalance())
                .build();
        transactionRepository.save(txReceiver);

        // 5. GL Posting (Liability Transfer)
        // Debit: Member Savings (Sender Liability Reduces) | Credit: Member Savings (Receiver Liability Increases)
        accountingService.postDoubleEntry(
                "Transfer " + fromAccountNum + " -> " + toAccountNum,
                txSender.getTransactionId(),
                "2002", // Debit Savings Control
                "2002", // Credit Savings Control
                amount
        );
    }

    /**
     * BATCH INTEREST POSTING
     */
    public void applyMonthlyInterest(BigDecimal annualRatePercentage) {
        List<SavingsAccount> accounts = savingsAccountRepository.findAll();
        BigDecimal monthlyRate = annualRatePercentage.divide(BigDecimal.valueOf(1200), 4, BigDecimal.ROUND_HALF_UP); // 5% / 100 / 12

        for (SavingsAccount acc : accounts) {
            if (acc.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal interest = acc.getBalance().multiply(monthlyRate).setScale(2, BigDecimal.ROUND_HALF_UP);

                if (interest.compareTo(BigDecimal.ZERO) > 0) {
                    // Credit Account
                    acc.setBalance(acc.getBalance().add(interest));
                    savingsAccountRepository.save(acc);

                    // Transaction Record
                    Transaction tx = Transaction.builder()
                            .member(acc.getMember())
                            .savingsAccount(acc)
                            .type(Transaction.TransactionType.INTEREST_EARNED)
                            .paymentMethod(Transaction.PaymentMethod.SYSTEM)
                            .amount(interest)
                            .description("Monthly Interest Earned")
                            .balanceAfter(acc.getBalance())
                            .build();
                    transactionRepository.save(tx);

                    // GL Posting
                    // Debit: Interest Expense (5006 - Bank Charges or create 5013 Interest Expense) | Credit: Member Savings (2002)
                    try {
                        accountingService.postDoubleEntry(
                                "Interest - " + acc.getAccountNumber(),
                                tx.getTransactionId(),
                                "5006", // Using Bank Charges/Interest Expense account
                                "2002", // Member Savings
                                interest
                        );
                    } catch (Exception e) {
                        System.err.println("GL Post Failed for Interest: " + e.getMessage());
                    }
                }
            }
        }
    }

    public BigDecimal getTotalSavingsBalance() {
        BigDecimal total = savingsAccountRepository.getTotalActiveAccountsBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    private String generateAccountNumber() {
        long count = savingsAccountRepository.count();
        return "SAV" + String.format("%06d", count + 1);
    }

    private SavingsAccountDTO convertToDTO(SavingsAccount account) {
        return SavingsAccountDTO.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .memberId(account.getMember().getId())
                .memberName(account.getMember().getFirstName() + " " + account.getMember().getLastName())
                .balance(account.getBalance())
                .totalDeposits(account.getTotalDeposits())
                .totalWithdrawals(account.getTotalWithdrawals())
                .status(account.getStatus().toString())
                .build();
    }
}