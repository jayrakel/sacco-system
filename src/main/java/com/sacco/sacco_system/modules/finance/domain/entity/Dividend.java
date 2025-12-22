package com.sacco.sacco_system.modules.finance.domain.entity;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dividend Entity
 * Represents dividend declaration and allocation to members
 */
@Entity
@Table(name = "dividends")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dividend {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private Integer fiscalYear;

    private LocalDate declarationDate;

    private BigDecimal totalDividendPool;

    private BigDecimal memberSharePercentage;

    private BigDecimal dividendAmount;

    @Enumerated(EnumType.STRING)
    private DividendStatus status = DividendStatus.DECLARED;

    private LocalDate paymentDate;

    private String paymentReference;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DividendStatus {
        DECLARED,   // Dividends declared but not paid
        PAID,       // Dividends paid to member
        CANCELLED   // Dividend declaration cancelled
    }
}

