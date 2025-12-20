package com.sacco.sacco_system.modules.finance.domain.entity;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cash_flow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String transactionReference; // e.g., CF-2024-001

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlowDirection direction; // INFLOW or OUTFLOW

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member; // Null for non-member transactions

    private String description;

    // Link to related entity
    private UUID relatedEntityId; // Loan ID, Withdrawal ID, etc.

    @Enumerated(EnumType.STRING)
    private RelatedEntityType relatedEntityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    // Payment details
    private String mpesaCode; // For M-Pesa transactions
    private String chequeNumber; // For cheque payments
    private String bankReference; // For bank transfers

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "recorded_by")
    private UUID recordedBy; // User who recorded this

    private String notes;

    @PrePersist
    protected void onCreate() {
        transactionDate = LocalDateTime.now();
        if (transactionReference == null) {
            transactionReference = "CF-" + System.currentTimeMillis();
        }
    }

    public enum TransactionType {
        // INFLOWS (Money coming in)
        SHARE_CONTRIBUTION,      // Member buys shares
        SAVINGS_DEPOSIT,         // Member deposits savings
        LOAN_REPAYMENT,          // Member repays loan
        LOAN_APPLICATION_FEE,    // Member pays loan application fee
        MEMBERSHIP_FEE,          // New member registration fee
        FINE_PAYMENT,            // Member pays fine
        INTEREST_INCOME,         // Interest earned on loans
        EXTERNAL_GRANT,          // External funding/grants

        // OUTFLOWS (Money going out)
        LOAN_DISBURSEMENT,       // Money given to member as loan
        WITHDRAWAL,              // Member withdraws savings
        REFUND,                  // Refund to member
        OPERATIONAL_EXPENSE,     // Office rent, salaries, etc.
        BANK_CHARGES,            // Bank transaction fees

        // INTERNAL (No actual cash flow, just accounting)
        INTEREST_ACCRUAL         // Daily interest calculation (no cash movement)
    }

    public enum FlowDirection {
        INFLOW,   // Money coming into SACCO
        OUTFLOW   // Money leaving SACCO
    }

    public enum RelatedEntityType {
        LOAN,
        WITHDRAWAL,
        DEPOSIT,
        MEMBER,
        NONE
    }

    public enum PaymentMethod {
        MPESA,
        BANK_TRANSFER,
        CHEQUE,
        CASH,
        INTERNAL_TRANSFER
    }
}

