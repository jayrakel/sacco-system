package com.sacco.sacco_system.modules.admin.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Business Identifier (e.g., "AST-2025-001")
    @Column(nullable = false, unique = true)
    private String assetCode; // Renamed from tag

    @Column(nullable = false)
    private String assetName; // Renamed from name

    private String description;

    // --- Financials ---

    @Column(nullable = false)
    private LocalDate purchaseDate;

    // Historical Cost (Basis) - Immutable
    @Column(nullable = false, updatable = false)
    private BigDecimal purchaseCost;

    // Book Value (Updated via Depreciation Service)
    @Column(nullable = false)
    private BigDecimal currentValue;

    // Annual Depreciation % (e.g., 0.10)
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal depreciationRate = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AssetStatus assetStatus = AssetStatus.ACTIVE;

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
        if (assetStatus == null) assetStatus = AssetStatus.ACTIVE;

        // Initial Book Value = Purchase Cost (if not provided)
        if (currentValue == null && purchaseCost != null) {
            currentValue = purchaseCost;
        }

        // Fallback Generator
        if (assetCode == null) {
            assetCode = "AST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AssetStatus {
        ACTIVE,
        DISPOSED,
        WRITTEN_OFF,
        IN_REPAIR
    }
}