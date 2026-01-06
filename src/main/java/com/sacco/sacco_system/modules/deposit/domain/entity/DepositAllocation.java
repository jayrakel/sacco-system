package com.sacco.sacco_system.modules.deposit.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deposit_allocations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Composition: Link to Parent Deposit (Required for Lifecycle)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Deposit deposit;

    // --- Destinations (Loose Coupling via UUIDs) ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DestinationType destinationType;

    @Column(name = "savings_account_id")
    private UUID savingsAccountId; // Nullable

    @Column(name = "loan_id")
    private UUID loanId; // Nullable

    @Column(name = "fine_id")
    private UUID fineId; // Nullable

    @Column(name = "deposit_product_id")
    private UUID depositProductId; // Nullable (For specific products)

    // --- Financials ---

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AllocationStatus allocationStatus = AllocationStatus.PENDING;

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
        if (allocationStatus == null) allocationStatus = AllocationStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DestinationType {
        SAVINGS,
        LOAN,
        FINE,
        CONTRIBUTION,
        SHARE_CAPITAL
    }

    public enum AllocationStatus {
        PENDING,
        PROCESSED,
        FAILED
    }
}