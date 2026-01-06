package com.sacco.sacco_system.modules.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dividends")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dividend {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Structural: Decoupled Member
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    // Accounting Period
    @Column(nullable = false)
    private Integer fiscalYear; // e.g., 2025

    // Financial Link (Nullable until Payout)
    @Column(name = "transaction_id")
    private UUID transactionId;

    // --- Financials ---

    @Column(nullable = false)
    private BigDecimal dividendRate; // The declared %

    @Column(nullable = false)
    private BigDecimal grossAmount;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal withholdingTax = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal netAmount; // Persisted for query speed

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DividendStatus dividendStatus = DividendStatus.CALCULATED;

    // --- Global Audit ---
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Integrity Check (Transient Helper)
    @Transient
    public boolean isValid() {
        if (grossAmount == null || withholdingTax == null || netAmount == null) return false;
        return netAmount.compareTo(grossAmount.subtract(withholdingTax)) == 0;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (dividendStatus == null) dividendStatus = DividendStatus.CALCULATED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DividendStatus {
        CALCULATED,
        POSTED, // Journal Entry Created
        PAID    // Disbursed to Member
    }
}