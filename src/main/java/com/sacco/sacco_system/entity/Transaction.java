package com.sacco.sacco_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(unique = true, nullable = false)
    private String transactionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id")
    @JsonIgnoreProperties({"transactions", "loans", "savingsAccounts", "guaranteedLoans"})
    private Member member;

    @ManyToOne
    @JoinColumn(name = "savings_account_id")
    @JsonIgnoreProperties("member")
    private SavingsAccount savingsAccount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String referenceCode; // M-Pesa Code or Check No
    private String description;

    @CreationTimestamp
    private LocalDateTime transactionDate;

    private BigDecimal balanceAfter;

    @PrePersist
    protected void onCreate() {
        if (transactionId == null) {
            transactionId = "TRX-" + System.currentTimeMillis();
        }
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        REGISTRATION_FEE,
        LOAN_DISBURSEMENT,
        LOAN_REPAYMENT,

        // ✅ NEW TYPES ADDED
        TRANSFER,       // Member to Member
        REVERSAL,       // Correction
        INTEREST_EARNED, // Savings Interest
        BANK_CHARGE      // Fees
    }

    public enum PaymentMethod {
        CASH,
        BANK_TRANSFER,
        MPESA,
        CHECK,
        SYSTEM // For automated things like Interest
    }
}