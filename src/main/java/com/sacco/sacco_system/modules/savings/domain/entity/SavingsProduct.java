package com.sacco.sacco_system.modules.savings.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "savings_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ✅ Rule Section 2: productCode must be Globally Unique
    @Column(nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false)
    private String productName;

    private String description;

    // ✅ Rule Section 15: currencyCode required.
    // Removed @Builder.Default to enforce 'KES' on JSON deserialization (No-Args constructor)
    @Column(nullable = false)
    private String currencyCode = "KES";

    @Enumerated(EnumType.STRING)
    private ProductType type;

    private BigDecimal interestRate;

    private BigDecimal minBalance;

    private Integer minDurationMonths;

    private boolean allowWithdrawal;

    // ✅ Rule Section 1: Global Audit Fields
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

        // ✅ Fix: Auto-generate robust Unique ID if missing (Complies with Section 2)
        if (this.productCode == null || this.productCode.isEmpty()) {
            this.productCode = "SAV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        // ✅ Fix: Enforce Default Currency (Complies with Section 15)
        if (this.currencyCode == null) {
            this.currencyCode = "KES";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ProductType {
        SAVINGS, FIXED_DEPOSIT, RECURRING_DEPOSIT
    }
}