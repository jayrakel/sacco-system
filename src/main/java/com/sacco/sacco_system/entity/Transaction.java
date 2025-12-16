package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Unique reference for the transaction (e.g., TXN123456789)
    private String transactionId;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "savings_account_id")
    private SavingsAccount savingsAccount;

    // Optional: Link to a loan if this transaction is related to one
    @ManyToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private BigDecimal amount;

    private String description;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String referenceCode; // External ref (e.g. M-Pesa code, Check No)

    private BigDecimal balanceAfter;

    private LocalDateTime transactionDate;

    @PrePersist
    protected void onCreate() {
        transactionDate = LocalDateTime.now();
        if (transactionId == null) {
            transactionId = "TXN" + System.currentTimeMillis();
        }
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER,
        INTEREST_EARNED,
        REGISTRATION_FEE,
        SHARE_PURCHASE,

        // ✅ LOAN RELATED TYPES
        PROCESSING_FEE,         // Application/Processing Fee
        LOAN_DISBURSEMENT,      // Money out to member
        LOAN_REPAYMENT,         // Money in from member
        LATE_PAYMENT_PENALTY,   // Penalty charge

        REVERSAL                // Correction
    }

    public enum PaymentMethod {
        CASH,
        BANK_TRANSFER,
        MPESA,
        CHECK,
        SYSTEM
    }
}