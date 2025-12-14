package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false, unique = true)
    private String transactionId;

    private String referenceCode;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // ✅ FIX: Removed 'nullable = false'.
    // Registration Fees do NOT link to a savings account.
    @ManyToOne
    @JoinColumn(name = "savings_account_id", nullable = true)
    private SavingsAccount savingsAccount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private LocalDateTime transactionDate;

    private String description;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.transactionDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        if (this.transactionId == null) {
            this.transactionId = "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, LOAN_DISBURSEMENT, LOAN_REPAYMENT, REGISTRATION_FEE, FINE, DIVIDEND_PAYOUT
    }

    public enum PaymentMethod {
        CASH, MPESA, BANK_TRANSFER
    }
}