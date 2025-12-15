package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "charges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    private Loan loan; // Optional: If linked to a loan

    @Enumerated(EnumType.STRING)
    private ChargeType type;

    private BigDecimal amount;
    private String description;

    @Enumerated(EnumType.STRING)
    private ChargeStatus status = ChargeStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Tracks if this charge was waived/overridden
    private boolean isWaived = false;
    private String waiverReason;

    public enum ChargeType {
        LATE_PAYMENT_PENALTY,
        PROCESSING_FEE,
        REGISTRATION_FEE,
        INSURANCE_FEE,
        WITHDRAWAL_FEE
    }

    public enum ChargeStatus {
        PENDING, PAID, WAIVED
    }
}