package com.sacco.sacco_system.entity.accounting;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "gl_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GLAccount {

    @Id
    @Column(length = 20)
    private String code; // e.g., "1001" for Cash, "4001" for Interest Income

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private AccountType type;

    private BigDecimal balance = BigDecimal.ZERO; // Running Balance

    private boolean active = true;
}