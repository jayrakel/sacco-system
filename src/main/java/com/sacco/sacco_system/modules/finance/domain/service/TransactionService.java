package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.sacco.sacco_system.modules.member.domain.entity.Member;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final AccountingService accountingService;

    public List<Transaction> getAllTransactions(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            // Fetch transactions within the range
            return transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(
                    startDate.atStartOfDay(),
                    endDate.atTime(LocalTime.MAX)
            );
        }
        // Default: Return all transactions sorted by latest
        return transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "transactionDate"));
    }

    /**
     * REVERSE A TRANSACTION
     * Undo a Deposit or Withdrawal, creating a Reversal Record and GL entry.
     */
    @Transactional
    public void reverseTransaction(String transactionId, String reason) {
        Transaction originalTx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (originalTx.getType() == Transaction.TransactionType.REVERSAL) {
            throw new RuntimeException("Cannot reverse a reversal.");
        }

        // Logic for Savings Reversal
        if (originalTx.getSavingsAccount() != null) {
            SavingsAccount acc = originalTx.getSavingsAccount();
            BigDecimal amount = originalTx.getAmount();

            if (originalTx.getType() == Transaction.TransactionType.DEPOSIT) {
                // Reversing Deposit -> Deduct money
                if (acc.getBalanceAmount().compareTo(amount) < 0) {
                    throw new RuntimeException("Cannot reverse: Insufficient balance.");
                }
                acc.setBalanceAmount(acc.getBalanceAmount().subtract(amount));
                acc.setTotalDeposits(acc.getTotalDeposits().subtract(amount));

                // GL Reversal: Debit Savings (2002), Credit Cash (1001)
                accountingService.postDoubleEntry("Reversal: " + transactionId, "REV-" + transactionId, "2002", "1001", amount);

            } else if (originalTx.getType() == Transaction.TransactionType.WITHDRAWAL) {
                // Reversing Withdrawal -> Add money back
                acc.setBalanceAmount(acc.getBalanceAmount().add(amount));
                acc.setTotalWithdrawals(acc.getTotalWithdrawals().subtract(amount));

                // GL Reversal: Debit Cash (1001), Credit Savings (2002)
                accountingService.postDoubleEntry("Reversal: " + transactionId, "REV-" + transactionId, "1001", "2002", amount);
            }

            savingsAccountRepository.save(acc);
        } else {
            throw new RuntimeException("Reversal not supported for this transaction type yet.");
        }

        // Create Reversal Record
        Transaction reversalTx = Transaction.builder()
                .member(originalTx.getMember())
                .savingsAccount(originalTx.getSavingsAccount())
                .type(Transaction.TransactionType.REVERSAL)
                .paymentMethod(Transaction.PaymentMethod.SYSTEM)
                .amount(originalTx.getAmount()) // Keep positive for record keeping
                .referenceCode("REV-" + originalTx.getReferenceCode())
                .description("Reversal of " + originalTx.getTransactionId() + ": " + reason)
                .balanceAfter(originalTx.getSavingsAccount().getBalanceAmount())
                .build();

        transactionRepository.save(reversalTx);
    }

    public ByteArrayInputStream generateCsvStatement() {
        // Generate CSV for all transactions (could be optimized to use filters later)
        List<Transaction> transactions = getAllTransactions(null, null); 
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder csv = new StringBuilder();
        // CSV Header
        csv.append("Transaction ID,Date,Member,Type,Method,Reference,Amount,Description\n");

        // CSV Rows
        for (Transaction tx : transactions) {
            String memberName = (tx.getMember() != null)
                    ? tx.getMember().getFirstName() + " " + tx.getMember().getLastName()
                    : "N/A";

            csv.append(tx.getTransactionId()).append(",");
            csv.append(tx.getTransactionDate().format(formatter)).append(",");
            csv.append(escapeSpecialCharacters(memberName)).append(",");
            csv.append(tx.getType()).append(",");
            csv.append(tx.getPaymentMethod()).append(",");
            csv.append(tx.getReferenceCode()).append(",");
            csv.append(tx.getAmount()).append(",");
            csv.append(escapeSpecialCharacters(tx.getDescription())).append("\n");
        }

        return new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String escapeSpecialCharacters(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    /**
     * Record processing fee payment for new loan applications
     */
    @Transactional
    public void recordProcessingFee(BigDecimal amount, String referenceCode, String type) {
        // Get current authenticated member from security context
        Member member = getCurrentMember();

        // Create transaction record for the processing fee
        Transaction transaction = Transaction.builder()
                .member(member)
                .amount(amount)
                .type(Transaction.TransactionType.PROCESSING_FEE)
                .paymentMethod(Transaction.PaymentMethod.MPESA)
                .referenceCode(referenceCode)
                .description("Loan application processing fee")
                .build();

        transactionRepository.save(transaction);

        // Post to accounting
        accountingService.postEvent("PROCESSING_FEE",
                "Loan Application Fee - " + member.getMemberNumber(),
                referenceCode,
                amount);
    }

    private Member getCurrentMember() {
        // Get the authenticated user from Spring Security context
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof com.sacco.sacco_system.modules.users.domain.entity.User) {
            com.sacco.sacco_system.modules.users.domain.entity.User user =
                (com.sacco.sacco_system.modules.users.domain.entity.User) auth.getPrincipal();
            // Fetch member by email (User and Member are independent but share email)
            if (user.getEmail() != null) {
                return savingsAccountRepository.findAll().stream()
                    .map(SavingsAccount::getMember)
                    .filter(m -> m.getEmail().equals(user.getEmail()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Member not found"));
            }
        }
        throw new RuntimeException("Unable to identify current member");
    }

    /**
     * ✅ UPDATED: Dynamic Fee Recording
     * @param incomeGlCode Optional override. If provided (from LoanProduct), it replaces the default Credit account.
     */
    @Transactional
    public void recordProcessingFee(Member member, BigDecimal amount, String referenceCode, String incomeGlCode) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;

        // 1. Create Transaction Record (Audit Trail)
        Transaction transaction = Transaction.builder()
                .member(member)
                .amount(amount)
                .type(Transaction.TransactionType.PROCESSING_FEE)
                .paymentMethod(Transaction.PaymentMethod.MPESA) // Assuming mobile money for "There and Then"
                .referenceCode(referenceCode)
                .description("Loan Application Fee Payment")
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);

        // 2. Post to Accounting (Dynamic)
        // We use "LOAN_APPLICATION_FEE" to look up defaults (Dr: 1002, Cr: 4200).
        // If 'incomeGlCode' is passed, it overrides the Credit side (Income).
        accountingService.postEvent(
                "LOAN_APPLICATION_FEE",
                "Loan App Fee: " + referenceCode,
                referenceCode,
                amount,
                null,          // Default Debit (Use mapping's 1002)
                incomeGlCode   // Override Credit (Use Product's Income Code if available)
        );
    }
}