package com.sacco.sacco_system.modules.loan.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_repayments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRepayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    /**
     * ✅ Matches Builder call: .installmentNumber(int)
     */
    private Integer installmentNumber;

    /**
     * ✅ Matches Builder call: .amountDue(BigDecimal)
     */
    private BigDecimal amountDue;

    @Builder.Default
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal interestPaid = BigDecimal.ZERO;

    /**
     * ✅ Renamed to amountPaid to fix Builder error: amountPaid(java.math.BigDecimal)
     */
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RepaymentStatus status = RepaymentStatus.PENDING;

    private LocalDate dueDate;
    private LocalDate paymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (principalPaid == null) principalPaid = BigDecimal.ZERO;
        if (interestPaid == null) interestPaid = BigDecimal.ZERO;
        if (amountPaid == null) amountPaid = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RepaymentStatus {
        PENDING,
        PARTIALLY_PAID,
        PAID,
        OVERDUE,
        DEFAULTED
    }

    /**
     * ✅ Explicit Helpers for backward compatibility with Service logic
     */
    public Integer getRepaymentNumber() {
        return this.installmentNumber;
    }

    public BigDecimal getAmount() {
        return this.amountDue;
    }

    public BigDecimal getTotalPaid() {
        return this.amountPaid;
    }
}