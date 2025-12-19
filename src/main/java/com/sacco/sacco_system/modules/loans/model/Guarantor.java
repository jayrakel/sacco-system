package com.sacco.sacco_system.modules.loans.model;

import com.sacco.sacco_system.modules.members.model.Member;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "guarantors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guarantor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    private BigDecimal guaranteeAmount;

    @Enumerated(EnumType.STRING)
    private GuarantorStatus status;

    private LocalDate dateRequestSent;
    private LocalDate dateResponded;

    public enum GuarantorStatus {
        PENDING, ACCEPTED, DECLINED
    }
}