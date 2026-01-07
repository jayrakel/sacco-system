package com.sacco.sacco_system.modules.deposit.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deposit_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositProduct {

    // Global Definition: Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Domain Dictionary: Key Fields
    @Column(nullable = false, unique = true)
    private String productCode; // e.g., "DEP-MBR-01"

    @Column(nullable = false, unique = true)
    private String productName; // Renamed from name

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    // --- Configuration (Product Rules) ---

    // Minimum amount accepted per transaction
    @Builder.Default
    private BigDecimal minAmount = BigDecimal.ZERO;

    // Maximum amount accepted (Optional limit)
    private BigDecimal maxAmount;

    // Flag: Is this a mandatory contribution? (e.g. Monthly Shares)
    @Column(nullable = false)
    @Builder.Default
    private boolean isMandatory = false;

    // --- Global Audit ---
    @Column(nullable = false)
    private boolean active = true; // Replaces status Enum

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy; // Decoupled from Member entity
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Fallback Generator
        if (productCode == null) {
            productCode = "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}