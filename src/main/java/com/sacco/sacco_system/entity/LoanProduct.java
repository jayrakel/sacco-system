package com.sacco.sacco_system.entity;

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
    private String name; // e.g., "Development Loan", "Emergency Loan"

    private String description;

    private BigDecimal interestRate; // Annual Rate (e.g., 12.00)

    @Enumerated(EnumType.STRING)
    private InterestType interestType; // FLAT or REDUCING_BALANCE

    private Integer maxTenureMonths; // e.g., 36 months

    private BigDecimal maxLimit; // e.g., 5,000,000

    public enum InterestType {
        FLAT_RATE,
        REDUCING_BALANCE
    }
}