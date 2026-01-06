package com.sacco.sacco_system.modules.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "charges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Mandatory: Who is charged
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    // Optional: Payment Transaction Link
    @Column(name = "transaction_id")
    private UUID transactionId;

    // Optional: Ad-hoc Loan Context
    @Column(name = "loan_id")
    private UUID loanId;

    // --- Financials ---

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    // --- Metadata ---

    @Column(nullable = false)
    private LocalDateTime chargeDate;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeType chargeType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChargeStatus chargeStatus = ChargeStatus.PENDING;

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
        if (chargeDate == null) chargeDate = LocalDateTime.now();
        if (chargeStatus == null) chargeStatus = ChargeStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ChargeType {
        SMS,
        STATEMENT,
        REGISTRATION,
        WITHDRAWAL_FEE,
        LEGAL_FEE,
        OTHER
    }

    public enum ChargeStatus {
        PENDING,
        PAID,
        WAIVED
    }
}