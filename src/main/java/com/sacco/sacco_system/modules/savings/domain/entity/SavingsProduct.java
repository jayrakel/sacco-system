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

    @Column(nullable = false, unique = true)
    private String productCode; // Unique business identifier (e.g., "SAV001")

    @Column(nullable = false)
    private String productName; // e.g., "Ordinary Savings", "Welfare Fund", "Holiday Account"

    private String description;

    @Column(nullable = false)
    @Builder.Default
    private String currencyCode = "KES"; // ISO 4217 currency code

    @Enumerated(EnumType.STRING)
    private ProductType type; // SAVINGS, FIXED_DEPOSIT, RECURRING_DEPOSIT

    private BigDecimal interestRate; // Annual % (e.g., 5.0 for 5%)

    private BigDecimal minBalance; // e.g., 500

    private Integer minDurationMonths; // Lock-in period (e.g., 12 months for Holiday)

    private boolean allowWithdrawal; // False for Welfare/Locked accounts

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

    public enum ProductType {
        SAVINGS, FIXED_DEPOSIT, RECURRING_DEPOSIT
    }
}



