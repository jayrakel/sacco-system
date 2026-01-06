package com.sacco.sacco_system.modules.loan.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_guarantors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Guarantor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Structural Change: Decoupled Loan Entity -> UUID
    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    // Structural Change: Decoupled Member Entity -> UUID
    // Renamed to guarantorMemberId per Section 19
    @Column(name = "guarantor_member_id", nullable = false)
    private UUID guarantorMemberId;

    // Renamed: guaranteeAmount -> guaranteedAmount
    @Column(nullable = false)
    private BigDecimal guaranteedAmount;

    // Renamed: status -> guarantorStatus
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GuarantorStatus guarantorStatus = GuarantorStatus.PENDING;

    // Global Definition: Audit & Identity
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (guarantorStatus == null) {
            guarantorStatus = GuarantorStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum GuarantorStatus {
        PENDING,
        ACCEPTED,
        DECLINED
    }
}