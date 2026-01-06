package com.sacco.sacco_system.modules.deposit.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "deposits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Structural: Decoupled Member
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    // Link to Financial Core (Nullable until PROCESSED)
    // This connects the 'Request' to the 'Ledger Entry'
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(nullable = false)
    private BigDecimal amount;

    // Renamed from date -> depositDate
    @Column(nullable = false)
    private LocalDateTime depositDate;

    // External Reference (e.g., M-Pesa Code)
    // Renamed from reference -> sourceReference
    @Column(name = "source_reference", unique = true)
    private String sourceReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod; // MPESA, BANK_TRANSFER, CASH, etc.

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DepositStatus depositStatus = DepositStatus.PENDING;

    // --- Allocations (The Split) ---
    // Maps the 1 Deposit -> N Destinations (Savings, Loan, Shares)
    @OneToMany(mappedBy = "deposit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DepositAllocation> allocations = new ArrayList<>();

    // --- Global Audit ---
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Helper for Allocations
    public void addAllocation(DepositAllocation allocation) {
        allocations.add(allocation);
        allocation.setDeposit(this);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (depositDate == null) depositDate = LocalDateTime.now();
        if (depositStatus == null) depositStatus = DepositStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DepositStatus {
        PENDING, PROCESSED, FAILED, REVERSED
    }

    public enum PaymentMethod {
        MPESA, BANK_TRANSFER, CASH, CHECK, SYSTEM
    }
}