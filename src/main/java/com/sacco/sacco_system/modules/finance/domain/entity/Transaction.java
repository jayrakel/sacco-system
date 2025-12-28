package com.sacco.sacco_system.modules.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
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

    // Internal unique ID (TXN...)
    private String transactionId;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "savings_account_id")
    private SavingsAccount savingsAccount;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private BigDecimal amount;

    private String description;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    // ✅ SYSTEM GENERATED CODE (e.g., REF-8X92M) - Used for verification
    private String referenceCode;

    // ✅ NEW: USER PROVIDED CODE (e.g., QKA...) - M-Pesa/Bank Reference
    private String externalReference;

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
        PROCESSING_FEE,
        LOAN_DISBURSEMENT,
        LOAN_REPAYMENT,
        LATE_PAYMENT_PENALTY,
        FINE_PAYMENT,
        REVERSAL
    }

    public enum PaymentMethod {
        CASH,
        BANK_TRANSFER,
        BANK,
        MPESA,
        CHECK,
        SYSTEM
    }
}