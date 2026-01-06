package com.sacco.sacco_system.modules.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Mandatory: Who is fined
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    // Optional: Context (e.g., Late Loan Repayment)
    @Column(name = "loan_id")
    private UUID loanId;

    // Optional: Link to the clearing transaction (if fully paid in one go)
    @Column(name = "transaction_id")
    private UUID transactionId;

    // --- Financials ---

    @Column(nullable = false, updatable = false)
    private BigDecimal amount; // Original Levied Amount

    @Column(nullable = false)
    @Setter(AccessLevel.PROTECTED)
    @Builder.Default
    private BigDecimal outstandingAmount = BigDecimal.ZERO; // Track balance

    @Column(nullable = false, length = 3)
    private String currencyCode;

    // --- Metadata ---

    @Column(nullable = false)
    private LocalDateTime fineDate;

    @Column(nullable = false)
    private String description; // Reason

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FineStatus fineStatus = FineStatus.PENDING;

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
        if (fineDate == null) fineDate = LocalDateTime.now();
        if (fineStatus == null) fineStatus = FineStatus.PENDING;
        // Default outstanding to total amount if not set
        if (outstandingAmount.compareTo(BigDecimal.ZERO) == 0 && amount != null) {
            outstandingAmount = amount;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum FineStatus {
        PENDING,
        PAID,
        WAIVED,
        WRITTEN_OFF
    }
}