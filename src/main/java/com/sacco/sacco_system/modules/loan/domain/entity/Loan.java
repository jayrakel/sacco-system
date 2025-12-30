package com.sacco.sacco_system.modules.loan.domain.entity;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

    @Builder.Default
    private BigDecimal loanBalance = BigDecimal.ZERO;

    // --- Terms ---
    // ✅ FIX: Renamed from 'duration' to 'durationWeeks' to match LoanApplicationService
    private Integer durationWeeks;

    private BigDecimal weeklyRepaymentAmount;

    // --- Status & Dates ---
    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    private LocalDate applicationDate;
    private LocalDate approvalDate;

    // ✅ Required by LoanRepaymentService
    private LocalDate disbursementDate;

    // --- Relationships ---
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Guarantor> guarantors = new ArrayList<>();

    // ✅ Statuses required by FineService & Frontend
    public enum LoanStatus {
        DRAFT,
        PENDING_GUARANTORS,
        SUBMITTED,
        APPROVED,
        ACTIVE,
        IN_ARREARS,
        DISBURSED,
        REJECTED,
        COMPLETED,
        DEFAULTED
    }
}