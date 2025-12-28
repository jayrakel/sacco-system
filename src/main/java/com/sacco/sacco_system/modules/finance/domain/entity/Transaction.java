package com.sacco.sacco_system.modules.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;

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

    // Loan link removed as part of loans module removal
    // private Loan loan;

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

        // NOTE: Loan-specific transaction types removed with loans module
        PROCESSING_FEE,         // Application/Processing Fee (used generically)
        LATE_PAYMENT_PENALTY,   // Penalty charge
        FINE_PAYMENT,           // Fine payment
        REVERSAL                // Correction
    }

    public enum PaymentMethod {
        CASH,
        BANK_TRANSFER,
        BANK,   // ✅ Added to support the new routing logic
        MPESA,
        CHECK,
        SYSTEM
    }
}