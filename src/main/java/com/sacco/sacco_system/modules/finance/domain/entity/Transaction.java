package com.sacco.sacco_system.modules.finance.domain.entity;

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

    // Domain Dictionary: Global Unique Constraint #9
    // Immutable Identity
    @Column(nullable = false, unique = true, updatable = false)
    private String transactionReference;

    // External Reference (e.g., M-Pesa Code) - Immutable
    @Column(updatable = false)
    private String externalReference;

    // --- Loose Coupling (Unified Ledger) ---
    // Immutable linkage to actors and accounts

    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @Column(name = "savings_account_id", updatable = false)
    private UUID savingsAccountId; // Nullable (e.g., if Loan only)

    @Column(name = "loan_id", updatable = false)
    private UUID loanId; // Nullable (e.g., if Savings only)

    // --- Financial Details ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currencyCode;

    @Column(nullable = false, updatable = false)
    private BigDecimal runningBalance;

    @Column(nullable = false, updatable = false)
    private String narration;

    @Column(nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    // --- Global Audit (Metadata) ---

    @Column(nullable = false)
    private boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (transactionReference == null) {
            transactionReference = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER,
        INTEREST_EARNED,
        REGISTRATION_FEE,
        SHARE_PURCHASE,
        PROCESSING_FEE,
        LOAN_DISBURSEMENT,
        LOAN_REPAYMENT,
        LATE_PAYMENT_PENALTY,
        FINE_PAYMENT,
        REVERSAL,
        CONTRA_ENTRY
    }

    public enum PaymentMethod {
        CASH,
        BANK_TRANSFER,
        MPESA,
        CHECK,
        SYSTEM
    }
}