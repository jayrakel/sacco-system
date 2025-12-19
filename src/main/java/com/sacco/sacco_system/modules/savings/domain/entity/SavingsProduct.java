package com.sacco.sacco_system.modules.savings.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsProduct;

@Entity
@Table(name = "savings_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "Ordinary Savings", "Welfare Fund", "Holiday Account"

    private String description;

    @Enumerated(EnumType.STRING)
    private ProductType type; // SAVINGS, FIXED_DEPOSIT, RECURRING_DEPOSIT

    private BigDecimal interestRate; // Annual % (e.g., 5.0 for 5%)

    private BigDecimal minBalance; // e.g., 500

    private Integer minDurationMonths; // Lock-in period (e.g., 12 months for Holiday)

    private boolean allowWithdrawal; // False for Welfare/Locked accounts

    public enum ProductType {
        SAVINGS, FIXED_DEPOSIT, RECURRING_DEPOSIT
    }
}



