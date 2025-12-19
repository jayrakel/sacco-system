package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.savings.domain.entity.Withdrawal;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.sacco.sacco_system.modules.member.domain.entity.Member;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final AccountingService accountingService;

    public List<Transaction> getAllTransactions() {
        // Return latest transactions first
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
                if (acc.getBalance().compareTo(amount) < 0) {
                    throw new RuntimeException("Cannot reverse: Insufficient balance.");
                }
                acc.setBalance(acc.getBalance().subtract(amount));
                acc.setTotalDeposits(acc.getTotalDeposits().subtract(amount));

                // GL Reversal: Debit Savings (2002), Credit Cash (1001)
                accountingService.postDoubleEntry("Reversal: " + transactionId, "REV-" + transactionId, "2002", "1001", amount);

            } else if (originalTx.getType() == Transaction.TransactionType.WITHDRAWAL) {
                // Reversing Withdrawal -> Add money back
                acc.setBalance(acc.getBalance().add(amount));
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
                .balanceAfter(originalTx.getSavingsAccount().getBalance())
                .build();

        transactionRepository.save(reversalTx);
    }

    public ByteArrayInputStream generateCsvStatement() {
        List<Transaction> transactions = getAllTransactions();
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
}





