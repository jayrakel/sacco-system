package com.sacco.sacco_system.modules.deposit.domain.entity;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DepositProduct Entity
 * Represents custom contribution products created by admins
 * Examples: Meat contribution, Harambee, Group projects, etc.
 */
@Entity
@Table(name = "deposit_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    private BigDecimal targetAmount;  // Optional target for the contribution

    private BigDecimal currentAmount = BigDecimal.ZERO;  // Total collected so far

    @Enumerated(EnumType.STRING)
    private DepositProductStatus status = DepositProductStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = true)
    private Member createdBy;  // Chairperson or Treasurer (null for system/admin users)

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currentAmount == null) {
            currentAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
