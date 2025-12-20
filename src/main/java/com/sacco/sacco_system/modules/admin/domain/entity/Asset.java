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

    private String name;

    private String category;  // e.g., "Furniture", "Electronics", "Vehicle"

    private String serialNumber;

    private BigDecimal purchaseCost;

    private LocalDate purchaseDate;

    private Integer usefulLifeYears;  // For depreciation calculation

    private BigDecimal salvageValue;  // Expected value at end of useful life

    private BigDecimal accumulatedDepreciation;

    private BigDecimal currentValue;  // purchaseCost - accumulatedDepreciation

    @Enumerated(EnumType.STRING)
    private AssetStatus status = AssetStatus.ACTIVE;

    private LocalDate disposalDate;

    private BigDecimal disposalValue;

    private String disposalNotes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currentValue == null) {
            currentValue = purchaseCost;
        }
        if (accumulatedDepreciation == null) {
            accumulatedDepreciation = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AssetStatus {
        ACTIVE,      // In use
        DISPOSED,    // Sold or discarded
        LOST         // Lost or stolen
    }
}



