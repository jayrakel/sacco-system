package com.sacco.sacco_system.modules.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "share_capital")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareCapital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Business Identifier (e.g., "SH-0001")
    @Column(nullable = false, unique = true)
    private String accountNumber;

    // Structural: Decoupled Member
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    // --- Financial Snapshot (Source of Truth is Ledger) ---
    // Setters restricted to enforce Atomic Transaction updates

    @Column(nullable = false)
    @Setter(AccessLevel.PROTECTED)
    @Builder.Default
    private BigDecimal shareCount = BigDecimal.ZERO; // Quantity

    @Column(nullable = false)
    @Setter(AccessLevel.PROTECTED)
    @Builder.Default
    private BigDecimal totalValue = BigDecimal.ZERO; // Monetary Value

    @Column(nullable = false, length = 3)
    private String currencyCode;

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
        if (shareCount == null) shareCount = BigDecimal.ZERO;
        if (totalValue == null) totalValue = BigDecimal.ZERO;
        // Fallback Generator
        if (accountNumber == null) {
            accountNumber = "SH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}