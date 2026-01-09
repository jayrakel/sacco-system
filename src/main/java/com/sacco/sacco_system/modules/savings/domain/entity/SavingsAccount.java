package com.sacco.sacco_system.modules.savings.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import com.sacco.sacco_system.modules.member.domain.entity.Member;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnoreProperties("savingsAccounts")
    private Member member;

    // âœ… NEW: Link to Product (Defines the rules)
    @ManyToOne
    @JoinColumn(name = "product_id")
    private SavingsProduct product;

    @Builder.Default
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private String currencyCode = "KES"; // ISO 4217 currency code

    @Builder.Default
    private BigDecimal totalDeposits = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    // âœ… NEW: For Fixed/Restricted Accounts
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    private LocalDateTime accountOpenDate;

    // Global Audit & Identity fields (Phase A requirement)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        accountOpenDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (balanceAmount == null) balanceAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AccountStatus {
        ACTIVE, DORMANT, CLOSED, FROZEN, MATURED
    }
}



