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
        IN_ARREARS,
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

    /**
     * ✅ CRITICAL FIX: Handles Draft loans (where duration is null)
     * and calculates date based on Disbursement + Grace Period.
     */
    public LocalDate getExpectedRepaymentDate() {
        // 1. Safety Check: If duration is missing (Draft stage), return null.
        if (this.duration == null || this.duration == 0) {
            return null;
        }

        // 2. Logic: If disbursed, start counting AFTER the grace period.
        if (this.disbursementDate != null) {
            int graceWeeks = (this.gracePeriodWeeks != null) ? this.gracePeriodWeeks : 0;
            LocalDate startDate = this.disbursementDate.plusWeeks(graceWeeks);

            return this.durationUnit == DurationUnit.WEEKS ?
                    startDate.plusWeeks(this.duration) :
                    startDate.plusMonths(this.duration);
        }

        // 3. Fallback for non-disbursed loans
        return this.applicationDate != null ? this.applicationDate.plusMonths(this.duration) : LocalDate.now();
    }
}