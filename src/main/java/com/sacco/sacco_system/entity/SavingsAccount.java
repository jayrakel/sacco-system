package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    // ✅ Using ManyToOne allows a member to have multiple accounts later (Savings, Holiday, Junior)
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder.Default // ✅ Ensures builder uses this default
    private BigDecimal balance = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalDeposits = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    private LocalDateTime accountOpenDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        accountOpenDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (balance == null) balance = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AccountStatus {
        ACTIVE, CLOSED, FROZEN
    }
}