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

    // Global Definition: Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Domain Dictionary: Key Fields (Section 2 & 15)
    @Column(nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false, unique = true)
    private String productName; // Renamed from name

    private String description;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    // -----------------------------------------------------------------
    // Configuration Fields (Product Factory Pattern - Authorized)
    // -----------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    private ProductType type; // SAVINGS, FIXED_DEPOSIT, RECURRING_DEPOSIT

    private BigDecimal interestRate;

    private BigDecimal minBalance;

    private Integer minDurationMonths;

    private boolean allowWithdrawal;
    // -----------------------------------------------------------------

    // Global Definition: Audit & Identity
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
        // Fallback generation if missing
        if (productCode == null) {
            productCode = "SAV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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