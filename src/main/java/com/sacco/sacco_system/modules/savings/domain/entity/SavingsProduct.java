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

    // Domain Dictionary: Key Fields
    @Column(nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false, unique = true)
    private String productName;

    private String description;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    // --- Configuration (Product Rules) ---

    @Enumerated(EnumType.STRING)
    private ProductType type; // SAVINGS, FIXED_DEPOSIT, RECURRING_DEPOSIT

    private BigDecimal interestRate; // Annual %

    private BigDecimal minBalance; // Required to keep open

    // Added: Max amount withdrawable per transaction/day (Risk Control)
    private BigDecimal withdrawalLimit;

    private Integer minDurationMonths;

    private boolean allowWithdrawal;

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