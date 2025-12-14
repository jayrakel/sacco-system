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
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private AccountType type;

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    // ✅ FIX 1: Use 'Boolean' (Wrapper) instead of 'boolean'
    // This allows Jackson to handle missing/null values without crashing
    @Builder.Default
    private Boolean active = true;

    // ✅ FIX 2: Add this helper method
    // Since we changed to 'Boolean', Lombok generates 'getActive()'.
    // This manual method ensures your Service code calling '.isActive()' still works.
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }
}