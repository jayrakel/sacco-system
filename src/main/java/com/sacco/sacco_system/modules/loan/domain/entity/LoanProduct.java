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

    // Global Definition: Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Domain Dictionary: Key Fields (Section 16)
    @Column(nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false, unique = true)
    private String productName; // Renamed from name

    private String description;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    // --- Terms ---
    @Column(nullable = false)
    private BigDecimal interestRate; // Annual %

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InterestType interestType = InterestType.FLAT; // Renamed from FLAT_RATE

    // Preserved: maxDurationWeeks (Dictionary uses Months, but logic preservation authorized)
    @Column(nullable = false)
    private Integer maxDurationWeeks;

    @Column(nullable = false)
    private BigDecimal maxAmount;

    // --- Fee Configurations ---
    @Column(name = "application_fee", nullable = false)
    @Builder.Default
    private BigDecimal applicationFee = BigDecimal.ZERO;

    @Column(name = "penalty_rate", nullable = false)
    @Builder.Default
    private BigDecimal penaltyRate = BigDecimal.ZERO;

    // --- Accounting Integration (Preserved) ---
    @Column(name = "receivable_account_code")
    private String receivableAccountCode;

    @Column(name = "income_account_code")
    private String incomeAccountCode;

    // Global Definition: Audit & Identity
    @Column(nullable = false)
    private boolean active = true; // Renamed from isActive

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (productCode == null) {
            productCode = "LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum InterestType {
        FLAT, // Renamed from FLAT_RATE
        REDUCING_BALANCE
    }
}