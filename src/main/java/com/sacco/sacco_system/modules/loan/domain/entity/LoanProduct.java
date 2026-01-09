package com.sacco.sacco_system.modules.loan.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false)
    private String productName;

    private String description;

    @Column(nullable = false)
    @Builder.Default
    private String currencyCode = "KES";

    // --- Terms ---
    @Column(nullable = false)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InterestType interestType = InterestType.FLAT;

    @Column(nullable = false)
    private Integer maxDurationWeeks;

    // ✅ ADDED: Minimum Loan Amount (Fixes the error)
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal minAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal maxAmount;

    // --- Fee Configurations ---
    @Column(name = "application_fee", nullable = false)
    @Builder.Default
    private BigDecimal applicationFee = BigDecimal.ZERO;

    @Column(name = "penalty_rate", nullable = false)
    @Builder.Default
    private BigDecimal penaltyRate = BigDecimal.ZERO;

    // --- Accounting ---
    @Column(name = "receivable_account_code")
    private String receivableAccountCode;

    @Column(name = "income_account_code")
    private String incomeAccountCode;

    // Audit
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum InterestType {
        FLAT,
        REDUCING_BALANCE
    }
}