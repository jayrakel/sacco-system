package com.sacco.sacco_system.service;

import com.sacco.sacco_system.entity.Transaction;
import com.sacco.sacco_system.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> getAllTransactions() {
        // Return latest transactions first
        return transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "transactionDate"));
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