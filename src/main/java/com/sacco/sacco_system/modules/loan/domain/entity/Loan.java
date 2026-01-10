package com.sacco.sacco_system.modules.loan.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct product;

    // --- Financials ---
    @Column(nullable = false)
    private BigDecimal principalAmount;

    @Column(nullable = false)
    private BigDecimal interestRate; // Locked at time of application

    @Column(nullable = false)
    @Builder.Default
    private String currencyCode = "KES"; // ISO 4217 currency code

    @Builder.Default
    private BigDecimal approvedAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal disbursedAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal outstandingPrincipal = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal outstandingInterest = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalOutstandingAmount = BigDecimal.ZERO;

    // --- Terms ---
    // ✅ FIX: Renamed from 'duration' to 'durationWeeks' to match LoanApplicationService
    private Integer durationWeeks;

    private BigDecimal weeklyRepaymentAmount;

    private LocalDate maturityDate;

    // --- Status & Dates ---
    @Enumerated(EnumType.STRING)
    private LoanStatus loanStatus;

    private LocalDate applicationDate;
    private LocalDate approvalDate;

    // ✅ Required by LoanRepaymentService
    private LocalDate disbursementDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean feePaid = false;

    // Global Audit & Identity fields (Phase A requirement)
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Relationships ---
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Guarantor> guarantors = new ArrayList<>();

    // ✅ Statuses required by FineService & Frontend - Aligned with Dictionary Phase B
    public enum LoanStatus {
        DRAFT,
        PENDING_GUARANTORS,
        AWAITING_GUARANTORS,
        SUBMITTED,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        CANCELLED,
        DISBURSED,
        ACTIVE,
        IN_ARREARS,
        DEFAULTED,
        CLOSED,
        WRITTEN_OFF
    }
}