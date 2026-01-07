package com.sacco.sacco_system.modules.savings.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "withdrawals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Mandatory: Who is withdrawing
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    // Mandatory: Source of Funds
    @Column(name = "savings_account_id", nullable = false)
    private UUID savingsAccountId;

    // Link to Financial Core (Nullable until PROCESSED)
    @Column(name = "transaction_id")
    private UUID transactionId;

    // --- Financials ---

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "withdrawal_fee")
    @Builder.Default
    private BigDecimal withdrawalFee = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    // --- Workflow Details ---

    @Column(nullable = false)
    private LocalDateTime requestDate;

    private LocalDateTime processedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalMethod withdrawalMethod;

    // Critical: Where the money is going (Phone Number / Bank Account)
    @Column(nullable = false)
    private String destinationReference;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WithdrawalStatus withdrawalStatus = WithdrawalStatus.PENDING;

    private String rejectionReason;

    // --- Global Audit ---
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (requestDate == null) requestDate = LocalDateTime.now();
        if (withdrawalStatus == null) withdrawalStatus = WithdrawalStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum WithdrawalMethod {
        MPESA,
        BANK_TRANSFER,
        CASH,
        CHECK
    }

    public enum WithdrawalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        PROCESSED, // Money actually sent/Disbursed
        FAILED
    }
}