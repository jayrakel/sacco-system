package com.sacco.sacco_system.modules.loan.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "loan_products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    // Terms
    @Column(nullable = false)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private Integer maxDurationWeeks;

    @Column(nullable = false)
    private BigDecimal maxAmount;

    // --- Accounting Integration ---
    // Links to GLAccount codes in the Finance Module
    @Column(name = "receivable_account_code")
    private String receivableAccountCode; // e.g. "1201" (Asset)

    @Column(name = "income_account_code")
    private String incomeAccountCode; // e.g. "4001" (Income)

    @Builder.Default
    private boolean isActive = true;
}