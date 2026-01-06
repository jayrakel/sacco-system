package com.sacco.sacco_system.modules.savings.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(unique = true, nullable = false)
    private String accountNumber;

    // Structural Change: Decoupled Member Entity -> UUID
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    // Structural Change: Decoupled Product Entity -> UUID
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    // Renamed: balance -> balanceAmount
    // Critical: Setter restricted to prevent accidental service-layer mutation.
    // Mutation must occur via atomic SavingsTransaction logic.
    @Column(nullable = false)
    @Setter(AccessLevel.PROTECTED)
    @Builder.Default
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    // Renamed/Added: currency -> currencyCode
    @Column(nullable = false, length = 3)
    private String currencyCode;

    // -----------------------------------------------------------------
    // Legacy / Derived Fields (Preserved with Warning per Phase F)
    // -----------------------------------------------------------------
    @Builder.Default
    private BigDecimal totalDeposits = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    private LocalDate maturityDate;

    private LocalDateTime accountOpenDate;
    // -----------------------------------------------------------------

    // Renamed: status -> accountStatus
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    // Global Definition: Audit & Identity
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (accountOpenDate == null) {
            accountOpenDate = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (balanceAmount == null) {
            balanceAmount = BigDecimal.ZERO;
        }
        if (accountStatus == null) {
            accountStatus = AccountStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Standardized Dictionary Enum
    public enum AccountStatus {
        ACTIVE,
        DORMANT,
        FROZEN,
        CLOSED
    }
}