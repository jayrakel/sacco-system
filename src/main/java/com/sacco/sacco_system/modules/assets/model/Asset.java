package com.sacco.sacco_system.modules.assets.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    private String name; // e.g., "Office Laptop HP"
    private String serialNumber;

    private BigDecimal purchaseCost;
    private LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    private AssetStatus status; // ACTIVE, DISPOSED, LOST

    public enum AssetStatus { ACTIVE, DISPOSED, LOST }
}