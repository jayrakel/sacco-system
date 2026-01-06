package com.sacco.sacco_system.modules.loan.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Domain Dictionary: Identifiers
    @Column(nullable = false, unique = true)
    private String loanNumber;

    // Structural Change: Loose Coupling
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    // Structural Change: Loose Coupling (Foreign Key to LoanProduct)
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    // --- Financials: Application & Approval ---

    @Column(nullable = false)
    private BigDecimal principalAmount; // Applied/Requested Amount

    private BigDecimal approvedAmount; // Set upon APPROVAL

    private BigDecimal disbursedAmount; // Set upon DISBURSEMENT

    @Column(nullable = false)
    private BigDecimal interestRate;

    // --- Financials: Ledger (Source of Truth for Balances) ---

    // Renamed from balance: Tracks remaining principal
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal outstandingPrincipal = BigDecimal.ZERO;

    // Renamed from interest: Tracks unpaid interest
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal outstandingInterest = BigDecimal.ZERO;

    // Computed: Total Outstanding (Principal + Interest)
    @Transient
    public BigDecimal getTotalOutstandingAmount() {
        return (outstandingPrincipal == null ? BigDecimal.ZERO : outstandingPrincipal)
                .add(outstandingInterest == null ? BigDecimal.ZERO : outstandingInterest);
    }

    // --- Dates ---

    private LocalDate applicationDate;
    private LocalDate approvalDate; // Added per Section 17
    private LocalDate disbursementDate;
    private LocalDate repaymentStartDate;
    private LocalDate maturityDate;

    // --- Status Lifecycle (Section 18) ---

    // Workflow Status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LoanApplicationStatus applicationStatus = LoanApplicationStatus.DRAFT;

    // Financial/Ledger Status
    @Enumerated(EnumType.STRING)
    private LoanStatus loanStatus; // Nullable until Disbursed

    // --- Audit ---
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
        if (applicationStatus == null) applicationStatus = LoanApplicationStatus.DRAFT;
        if (outstandingPrincipal == null) outstandingPrincipal = BigDecimal.ZERO;
        if (outstandingInterest == null) outstandingInterest = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Domain Dictionary: Section 18 - LoanApplicationStatus
    public enum LoanApplicationStatus {
        DRAFT,
        SUBMITTED,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        CANCELLED
    }

    // Domain Dictionary: Section 18 - LoanStatus
    public enum LoanStatus {
        DISBURSED,
        ACTIVE,
        IN_ARREARS,
        DEFAULTED,
        CLOSED,
        WRITTEN_OFF
    }
}