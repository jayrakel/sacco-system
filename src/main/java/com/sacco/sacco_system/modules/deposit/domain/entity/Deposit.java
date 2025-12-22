package com.sacco.sacco_system.modules.deposit.domain.entity;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Deposit Entity
 * Represents a deposit made by a member with routing to multiple destinations
 */
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

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private DepositStatus status = DepositStatus.PENDING;

    @Column(unique = true)
    private String transactionReference;  // For tracking/reconciliation

    private String paymentMethod;  // MPESA, BANK, CASH, etc.

    private String paymentReference;  // External reference (e.g., MPESA code)

    @OneToMany(mappedBy = "deposit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DepositAllocation> allocations = new ArrayList<>();

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionReference == null) {
            transactionReference = "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    public void addAllocation(DepositAllocation allocation) {
        allocations.add(allocation);
        allocation.setDeposit(this);
    }
}
