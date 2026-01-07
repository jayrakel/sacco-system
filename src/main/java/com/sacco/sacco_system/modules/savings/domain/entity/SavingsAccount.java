package com.sacco.sacco_system.modules.savings.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "savings_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Business Identifier (Unique)
    @Column(unique = true, nullable = false)
    private String accountNumber;

    // Structural: Decoupled Member
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    // Structural: Decoupled Product
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    // Financial Snapshot (Source of Truth is Ledger)
    // Protected Setter enforces atomic updates via Transaction Service
    @Column(nullable = false)
    @Setter(AccessLevel.PROTECTED)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    // --- Performance Snapshots (Derived from Ledger) ---
    // Useful for quick interest calculation without re-summing history

    @Builder.Default
    @Setter(AccessLevel.PROTECTED)
    private BigDecimal totalDeposits = BigDecimal.ZERO;

    @Builder.Default
    @Setter(AccessLevel.PROTECTED)
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;

    @Builder.Default
    @Setter(AccessLevel.PROTECTED)
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    private LocalDate maturityDate;
    private LocalDateTime accountOpenDate;

    // --- Status & Audit ---

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

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
        if (accountOpenDate == null) accountOpenDate = LocalDateTime.now();
        if (balance == null) balance = BigDecimal.ZERO;
        if (accountStatus == null) accountStatus = AccountStatus.ACTIVE;

        // Fallback Generator
        if (accountNumber == null) {
            accountNumber = "SA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AccountStatus {
        ACTIVE,
        DORMANT,
        FROZEN,
        CLOSED
    }
}