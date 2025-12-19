package com.sacco.sacco_system.modules.loans.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "loan_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    private BigDecimal interestRate; // Annual %

    @Enumerated(EnumType.STRING)
    private InterestType interestType;

    private Integer maxTenureMonths;

    private BigDecimal maxLimit;

    // ✅ NEW: Fee Configurations
    private BigDecimal processingFee; // Flat fee (e.g., 500)
    private BigDecimal penaltyRate;   // % of overdue amount (e.g., 10%)

    public enum InterestType {
        FLAT_RATE,
        REDUCING_BALANCE
    }
}