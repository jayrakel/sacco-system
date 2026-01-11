package com.sacco.sacco_system.modules.loan.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "loan_repayment_schedules", indexes = {
        @Index(name = "idx_schedule_loan_id", columnList = "loan_id"),
        @Index(name = "idx_schedule_due_date", columnList = "due_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private LocalDate dueDate;

    // Expected Amounts
    @Column(nullable = false)
    private BigDecimal principalDue;

    @Column(nullable = false)
    private BigDecimal interestDue;

    @Column(nullable = false)
    private BigDecimal totalDue;

    // Actual Payment Tracking
    @Column(nullable = false)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstallmentStatus status;

    public enum InstallmentStatus {
        PENDING,
        PARTIALLY_PAID,
        PAID,
        OVERDUE
    }
}