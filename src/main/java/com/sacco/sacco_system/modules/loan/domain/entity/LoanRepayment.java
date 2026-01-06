package com.sacco.sacco_system.modules.loan.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
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

    // Structural Change: Decoupled Loan Link
    @Column(name = "loan_id", nullable = false, updatable = false)
    private UUID loanId;

    // Critical Link to Financial Core (Transaction Entity)
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    // Redundant but required by instructions for Audit/Search
    @Column(name = "transaction_reference", nullable = false, updatable = false)
    private String transactionReference;

    @Column(nullable = false, updatable = false)
    private LocalDateTime paymentDate;

    // --- Financial Breakdown (Immutable) ---

    @Column(nullable = false, updatable = false)
    private BigDecimal amountPaid; // Total

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private BigDecimal principalComponent = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private BigDecimal interestComponent = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private BigDecimal feeComponent = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private BigDecimal penaltyComponent = BigDecimal.ZERO;

    // --- Global Audit ---
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
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}