package com.sacco.sacco_system.modules.loan.domain.entity;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core Loan Entity representing a member's loan application and active loan.
 */
@Entity
@Table(name = "loans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct product;

    @Column(nullable = false)
    private BigDecimal principalAmount;

    private BigDecimal loanBalance;
    private BigDecimal interestRate;
    private Integer duration;

    @Enumerated(EnumType.STRING)
    private DurationUnit durationUnit;

    private BigDecimal weeklyRepaymentAmount;
    private BigDecimal totalPrepaid;
    private BigDecimal totalArrears;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    private LocalDate applicationDate;
    private LocalDate submissionDate;
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private LocalDate meetingDate;

    private boolean applicationFeePaid;
    private String checkNumber;
    private Integer gracePeriodWeeks;
    
    // Governance / Voting Fields
    private boolean votingOpen;
    private Integer votesYes;
    private Integer votesNo;
    
    @ElementCollection
    private List<UUID> votedUserIds = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String secretaryComments;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Guarantor> guarantors = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanRepayment> repayments = new ArrayList<>();

    public enum LoanStatus {
        DRAFT,
        GUARANTORS_PENDING,
        GUARANTORS_APPROVED,
        APPLICATION_FEE_PENDING,
        SUBMITTED,
        LOAN_OFFICER_REVIEW,
        SECRETARY_TABLED,
        ON_AGENDA,
        VOTING_OPEN,
        VOTING_CLOSED,
        SECRETARY_DECISION,
        TREASURER_DISBURSEMENT,
        DISBURSED,
        ACTIVE,
        IN_ARREARS,      // ✅ Add this back
        ADMIN_APPROVED,
        REJECTED,
        DEFAULTED,
        COMPLETED,
        WRITTEN_OFF
    }

    public enum DurationUnit {
        WEEKS,
        MONTHS,
        YEARS
    }

    public String getMemberName() {
        return member != null ? member.getFirstName() + " " + member.getLastName() : "Unknown";
    }
    /** ✅ Added to resolve LoanAutomationService and LoanCalculatorService errors */
public LocalDate getExpectedRepaymentDate() {
    if (this.disbursementDate != null) {
        return this.durationUnit == DurationUnit.WEEKS ? 
               this.disbursementDate.plusWeeks(this.duration) : 
               this.disbursementDate.plusMonths(this.duration);
    }
    return this.applicationDate != null ? this.applicationDate.plusMonths(this.duration) : LocalDate.now();
}
}