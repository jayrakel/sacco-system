package com.sacco.sacco_system.modules.deposit.domain.entity;

import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.finance.domain.entity.Fine;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DepositAllocation Entity
 * Represents how a deposit is allocated to different destinations
 */
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

    @ManyToOne
    @JoinColumn(name = "deposit_id", nullable = false)
    private Deposit deposit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepositDestinationType destinationType;

    @Column(nullable = false)
    private BigDecimal amount;

    // Reference to the specific destination (only one will be set based on type)
    
    @ManyToOne
    @JoinColumn(name = "savings_account_id")
    private SavingsAccount savingsAccount;

    @ManyToOne
    @JoinColumn(name = "fine_id")
    private Fine fine;

    @ManyToOne
    @JoinColumn(name = "deposit_product_id")
    private DepositProduct depositProduct;

    @Enumerated(EnumType.STRING)
    private AllocationStatus status = AllocationStatus.PENDING;

    private String notes;

    private String errorMessage;  // If allocation fails
}
