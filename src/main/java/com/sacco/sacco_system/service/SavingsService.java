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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingsService {
    
    private final SavingsAccountRepository savingsAccountRepository;
    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    
    public SavingsAccountDTO createSavingsAccount(Long memberId) {
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
    
    public SavingsAccountDTO getSavingsAccountById(Long id) {
        SavingsAccount account = savingsAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Savings account not found"));
        return convertToDTO(account);
    }
    
    public SavingsAccountDTO getSavingsAccountByNumber(String accountNumber) {
        SavingsAccount account = savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Savings account not found"));
        return convertToDTO(account);
    }
    
    public List<SavingsAccountDTO> getSavingsAccountsByMemberId(Long memberId) {
        return savingsAccountRepository.findByMemberId(memberId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<SavingsAccountDTO> getAllSavingsAccounts() {
        return savingsAccountRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public SavingsAccountDTO deposit(String accountNumber, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be positive");
        }
        
        SavingsAccount account = savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Savings account not found"));
        
        account.setBalance(account.getBalance().add(amount));
        account.setTotalDeposits(account.getTotalDeposits().add(amount));
        
        SavingsAccount savedAccount = savingsAccountRepository.save(account);
        
        // Record transaction
        Transaction transaction = Transaction.builder()
                .savingsAccount(account)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .description(description != null ? description : "Deposit")
                .balanceAfter(savedAccount.getBalance())
                .build();
        
        transactionRepository.save(transaction);
        
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
        
        account.setBalance(account.getBalance().subtract(amount));
        account.setTotalWithdrawals(account.getTotalWithdrawals().add(amount));
        
        SavingsAccount savedAccount = savingsAccountRepository.save(account);
        
        // Record transaction
        Transaction transaction = Transaction.builder()
                .savingsAccount(account)
                .type(Transaction.TransactionType.WITHDRAWAL)
                .amount(amount)
                .description(description != null ? description : "Withdrawal")
                .balanceAfter(savedAccount.getBalance())
                .build();
        
        transactionRepository.save(transaction);
        
        return convertToDTO(savedAccount);
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
