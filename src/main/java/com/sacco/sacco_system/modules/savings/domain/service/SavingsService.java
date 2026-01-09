package com.sacco.sacco_system.modules.savings.domain.service;

import com.sacco.sacco_system.modules.savings.domain.entity.Withdrawal;
import com.sacco.sacco_system.modules.savings.api.dto.SavingsAccountDTO;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsProduct;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsProductRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingsService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final SavingsProductRepository savingsProductRepository;
    private final AccountingService accountingService;
    private final ReferenceCodeService referenceCodeService;
    
    // ✅ ADDED: Dependencies to check liabilities
    private final LoanRepository loanRepository;
    private final GuarantorRepository guarantorRepository;

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
                .balanceAmount(BigDecimal.ZERO)
                .totalDeposits(BigDecimal.ZERO)
                .totalWithdrawals(BigDecimal.ZERO)
                .accountStatus(SavingsAccount.AccountStatus.ACTIVE)
                .build();

        if (product.getMinDurationMonths() != null && product.getMinDurationMonths() > 0) {
            account.setMaturityDate(LocalDate.now().plusMonths(product.getMinDurationMonths()));
        }

        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            // Default opening deposit to CASH (1001) if not specified
            deposit(savedAccount.getAccountNumber(), initialDeposit, "Opening Deposit", "1001");
        }

        return convertToDTO(savedAccount);
    }

    public SavingsAccountDTO getSavingsAccountById(UUID id) {
        return convertToDTO(savingsAccountRepository.findById(id).orElseThrow(() -> new RuntimeException("Account not found")));
    }

    public SavingsAccountDTO getSavingsAccountByNumber(String accountNumber) {
        return convertToDTO(savingsAccountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Account not found")));
    }

    public List<SavingsAccountDTO> getMemberAccounts(UUID memberId) {
        return savingsAccountRepository.findByMemberId(memberId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

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

    /**
     * Overloaded deposit method for legacy calls (defaults to CASH/System)
     */
    public SavingsAccountDTO deposit(String accountNumber, BigDecimal amount, String description) {
        return deposit(accountNumber, amount, description, null);
    }

    /**
     * ✅ UPDATED: Deposit with Source Account Routing
     */
    public SavingsAccountDTO deposit(String accountNumber, BigDecimal amount, String description, String sourceAccountCode) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Amount must be positive");

        SavingsAccount account = savingsAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setBalanceAmount(account.getBalanceAmount().add(amount));
        account.setTotalDeposits(account.getTotalDeposits().add(amount));
        SavingsAccount savedAccount = savingsAccountRepository.save(account);

        Member member = account.getMember();
        member.setTotalSavings(member.getTotalSavings().add(amount));
        memberRepository.save(member);

        // Determine Payment Method for Transaction Record
        Transaction.PaymentMethod paymentMethod = Transaction.PaymentMethod.CASH;
        if (sourceAccountCode != null) {
            if (sourceAccountCode.equals("1002")) paymentMethod = Transaction.PaymentMethod.MPESA;
            else if (sourceAccountCode.startsWith("101")) paymentMethod = Transaction.PaymentMethod.BANK;
        }

        // Create transaction record
        Transaction tx = Transaction.builder()
                .savingsAccount(account)
                .member(member)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .description(description != null ? description : "Deposit")
                .balanceAfter(savedAccount.getBalanceAmount())
                .build();
        transactionRepository.save(tx);

        // ✅ POST TO ACCOUNTING with Source Account Override
        accountingService.postEvent(
            "SAVINGS_DEPOSIT",
            description != null ? description : "Savings Deposit - " + member.getMemberNumber(),
            "DEP-" + savedAccount.getId(), 
            amount,
            sourceAccountCode 
        );

        return convertToDTO(savedAccount);
    }

    /**
     * ✅ SECURED: MEMBER EXIT WITHDRAWAL
     * Prevents exit if member has active loans or is a guarantor.
     */
    public SavingsAccountDTO processMemberExit(UUID memberId, String reason) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 1. ✅ Check for Active Loans (Self)
        boolean hasActiveLoans = loanRepository.existsByMemberIdAndLoanStatusIn(
                memberId,
                List.of(
                    Loan.LoanStatus.ACTIVE,
                    Loan.LoanStatus.IN_ARREARS,
                    Loan.LoanStatus.DISBURSED
                )
        );

        if (hasActiveLoans) {
            throw new RuntimeException("Cannot exit SACCO: You have active unpaid loans. Please clear them first.");
        }

        // 2. ✅ Check for Active Guarantees (Others)
        boolean isActiveGuarantor = guarantorRepository.existsByMemberIdAndLoanLoanStatusIn(
                memberId,
                List.of(
                    Loan.LoanStatus.ACTIVE,
                    Loan.LoanStatus.IN_ARREARS,
                    Loan.LoanStatus.DISBURSED
                )
        );

        if (isActiveGuarantor) {
            throw new RuntimeException("Cannot exit SACCO: You are guaranteeing active loans for other members. You must be replaced as a guarantor before you can exit.");
        }

        // 3. Proceed with Account Closure
        List<SavingsAccount> accounts = savingsAccountRepository.findByMemberId(memberId);

        if (accounts.isEmpty()) {
            throw new RuntimeException("No savings accounts found for member");
        }

        BigDecimal totalWithdrawal = BigDecimal.ZERO;

        for (SavingsAccount account : accounts) {
            totalWithdrawal = totalWithdrawal.add(account.getBalanceAmount());
            account.setAccountStatus(SavingsAccount.AccountStatus.CLOSED);
            account.setBalanceAmount(BigDecimal.ZERO);
            savingsAccountRepository.save(account);
        }

        if (totalWithdrawal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("No funds available to withdraw");
        }

        member.setTotalSavings(BigDecimal.ZERO);
        member.setMemberStatus(Member.MemberStatus.EXITED);  // ✅ Changed from setStatus to setMemberStatus
        memberRepository.save(member);

        Transaction tx = Transaction.builder()
                .member(member)
                .type(Transaction.TransactionType.WITHDRAWAL)
                .amount(totalWithdrawal)
                .description("Member Exit: " + (reason != null ? reason : "Voluntary exit"))
                .balanceAfter(BigDecimal.ZERO)
                .build();
        transactionRepository.save(tx);

        Withdrawal withdrawal = Withdrawal.builder()
                .member(member)
                .amount(totalWithdrawal)
                .status(Withdrawal.WithdrawalStatus.APPROVED)
                .reason("Member Exit: " + (reason != null ? reason : "Voluntary exit"))
                .requestDate(LocalDateTime.now())
                .processingDate(LocalDateTime.now())
                .build();

        accountingService.postSavingsWithdrawal(withdrawal);

        return accounts.isEmpty() ? null : convertToDTO(accounts.get(0));
    }

    @Deprecated
    public SavingsAccountDTO withdraw(String accountNumber, BigDecimal amount, String description) {
        throw new RuntimeException("Regular withdrawals are not allowed. Members can only withdraw when exiting the SACCO.");
    }

    // ========================================================================
    // 3. INTEREST CALCULATION
    // ========================================================================

    public void applyMonthlyInterest() {
        List<SavingsAccount> accounts = savingsAccountRepository.findAll();
        for (SavingsAccount acc : accounts) {
            if (acc.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0 && acc.getAccountStatus() == SavingsAccount.AccountStatus.ACTIVE) {
                if (acc.getProduct() == null || acc.getProduct().getInterestRate() == null) continue;

                BigDecimal annualRate = acc.getProduct().getInterestRate();
                BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 8, RoundingMode.HALF_UP);
                BigDecimal interest = acc.getBalanceAmount().multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

                if (interest.compareTo(BigDecimal.ZERO) > 0) {
                    acc.setBalanceAmount(acc.getBalanceAmount().add(interest));
                    if(acc.getAccruedInterest() != null) {
                        acc.setAccruedInterest(acc.getAccruedInterest().add(interest));
                    }
                    savingsAccountRepository.save(acc);

                    Transaction tx = Transaction.builder()
                            .member(acc.getMember())
                            .savingsAccount(acc)
                            .type(Transaction.TransactionType.INTEREST_EARNED)
                            .amount(interest)
                            .paymentMethod(Transaction.PaymentMethod.SYSTEM)
                            .referenceCode(referenceCodeService.generateReferenceCode())
                            .description("Monthly Interest - " + acc.getAccountNumber())
                            .balanceAfter(acc.getBalanceAmount())
                            .build();
                    transactionRepository.save(tx);

                    try {
                        accountingService.postDoubleEntry(
                            "Interest " + acc.getAccountNumber(),
                            null,
                            "5006",
                            "2001",
                            interest
                        );
                    } catch (Exception e) {
                        // Log error but don't fail the interest calculation
                    }
                }
            }
        }
    }

    // ========================================================================
    // 4. HELPERS
    // ========================================================================

    private SavingsAccountDTO convertToDTO(SavingsAccount account) {
        String productName = (account.getProduct() != null) ? account.getProduct().getProductName() : "Ordinary Savings";
        BigDecimal rate = (account.getProduct() != null) ? account.getProduct().getInterestRate() : BigDecimal.ZERO;

        return SavingsAccountDTO.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .memberId(account.getMember().getId())
                .memberName(account.getMember().getFirstName() + " " + account.getMember().getLastName())
                .balanceAmount(account.getBalanceAmount())  // ✅ Changed from balance to balanceAmount
                .totalDeposits(account.getTotalDeposits())
                .totalWithdrawals(account.getTotalWithdrawals())
                .accountStatus(account.getAccountStatus().toString())  // ✅ Changed from status to accountStatus
                .productName(productName)
                .interestRate(rate)
                .maturityDate(account.getMaturityDate())
                .accruedInterest(account.getAccruedInterest())
                .build();
    }
}