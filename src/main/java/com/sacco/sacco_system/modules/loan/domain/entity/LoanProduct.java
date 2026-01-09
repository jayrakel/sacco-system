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
    private String productCode; // Unique business identifier (e.g., "LOAN001")

    @Column(nullable = false)
    private String productName;

    private String description;

    @Column(nullable = false)
    @Builder.Default
    private String currencyCode = "KES"; // ISO 4217 currency code

    // --- Terms ---
    @Column(nullable = false)
    private BigDecimal interestRate; // Annual %

    // ✅ FEATURE: Interest Calculation Logic
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InterestType interestType = InterestType.FLAT;

    @Column(nullable = false)
    private Integer maxDurationWeeks; // Standardized duration unit

    @Column(nullable = false)
    private BigDecimal maxAmount;     // Maximum loan limit

    // --- Fee Configurations ---
    @Column(name = "application_fee", nullable = false)
    @Builder.Default
    private BigDecimal applicationFee = BigDecimal.ZERO;

    @Column(name = "penalty_rate", nullable = false)
    @Builder.Default
    private BigDecimal penaltyRate = BigDecimal.ZERO;

    // --- Accounting Integration (The Admin's Job to Map These) ---
    @Column(name = "receivable_account_code")
    private String receivableAccountCode; // e.g. "1201" (Loan Portfolio Asset)

    @Column(name = "income_account_code")
    private String incomeAccountCode;     // e.g. "4001" (Interest Income)

    // Global Audit & Identity fields (Phase A requirement)
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

    // ✅ ENUM DEFINITION - Aligned with Dictionary Phase B
    public enum InterestType {
        FLAT,
        REDUCING_BALANCE
    }
}