package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @Column(unique = true, nullable = false)
    private String loanNumber;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct product;

    // --- Financials ---
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private BigDecimal totalInterest;
    private BigDecimal loanBalance; // Outstanding Balance

    // Installment Tracking
    private BigDecimal monthlyRepayment; // The standard installment amount
    private Integer duration;

    @Enumerated(EnumType.STRING)
    private DurationUnit durationUnit; // WEEKS or MONTHS

    // --- Repayment Engine ---
    private BigDecimal totalPrepaid = BigDecimal.ZERO; // Buffer for overpayments
    private BigDecimal totalArrears = BigDecimal.ZERO; // Buffer for underpayments
    private int gracePeriodWeeks;

    // --- Workflow Status ---
    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    // --- Flags ---
    private boolean applicationFeePaid;

    // --- Voting & Approval Metadata ---
    private LocalDate meetingDate;
    private boolean votingOpen;
    private int votesYes = 0;
    private int votesNo = 0;
    private String secretaryComments;
    private String rejectionReason;
    private String checkNumber; // Treasurer's Check/Receipt Ref

    // --- Dates ---
    private LocalDate applicationDate;
    private LocalDate submissionDate;
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private LocalDate expectedRepaymentDate; // Start of repayment

    // --- Relationships ---
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
    private List<LoanRepayment> repayments;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
    private List<Guarantor> guarantors;

    // ✅ THE MISSING ENUMS ARE DEFINED HERE
    public enum LoanStatus {
        DRAFT,                  // 1. Member editing
        GUARANTORS_PENDING,     // 2. Sent to guarantors (Waiting for them to accept)
        GUARANTORS_APPROVED,    // 3. All guarantors accepted
        APPLICATION_FEE_PENDING,// 4. Waiting for fee payment
        SUBMITTED,              // 5. Sent to Loan Officer
        LOAN_OFFICER_REVIEW,    // 6. Officer reviewing
        SECRETARY_TABLED,       // 7. Tabled for meeting
        ON_AGENDA,
        VOTING_OPEN,            // 8. Members voting
        VOTING_CLOSED,          // 9. Voting ended
        SECRETARY_DECISION,     // 10. Secretary calculating result
        ADMIN_APPROVED,         // 11. High-level approval
        TREASURER_DISBURSEMENT, // 12. Waiting for check/disbursement
        DISBURSED,              // 13. Money sent
        ACTIVE,                 // 14. Grace period over, repaying
        COMPLETED,              // 15. Fully paid
        DEFAULTED,              // 16. Failed to pay
        REJECTED,
        WRITTEN_OFF,
        APPROVED,               // Legacy/Simple status
        PENDING                 // Legacy/Simple status
    }

    public enum DurationUnit {
        WEEKS, MONTHS
    }

    public String getMemberName() {
        return member != null ? member.getFirstName() + " " + member.getLastName() : "Unknown";
    }
}