package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "gl_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GLAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code; // e.g., "1000"

    @Column(nullable = false)
    private String name; // e.g., "Cash on Hand"

    @Enumerated(EnumType.STRING)
    private AccountType type; // ASSET, LIABILITY, EQUITY, INCOME, EXPENSE

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    private String description;

    public enum AccountType {
        ASSET, LIABILITY, EQUITY, INCOME, EXPENSE
    }
}