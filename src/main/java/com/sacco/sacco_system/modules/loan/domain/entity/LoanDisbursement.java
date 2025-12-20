package com.sacco.sacco_system.modules.loan.domain.entity;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_disbursements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String disbursementNumber; // e.g., DISB-2024-001

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisbursementMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisbursementStatus status;

    // Cheque details (if method is CHEQUE)
    private String chequeNumber;
    private String bankName;
    private LocalDate chequeDate;
    private String payableTo; // Member name

    // Bank transfer details (if method is BANK_TRANSFER)
    private String accountNumber;
    private String accountName;
    private String bankCode;
    private String transactionReference;

    // M-Pesa details (if method is MPESA)
    private String mpesaPhoneNumber;
    private String mpesaTransactionId;

    // Cash details (if method is CASH)
    private String receivedBy; // Member signature/ID
    private String witnessedBy; // Staff who witnessed

    // Approval tracking
    @Column(name = "prepared_by")
    private UUID preparedBy; // User ID (usually Treasurer)

    @Column(name = "prepared_at")
    private LocalDateTime preparedAt;

    @Column(name = "approved_by")
    private UUID approvedBy; // User ID (usually Chairperson/Admin)

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "disbursed_by")
    private UUID disbursedBy; // User ID who completed disbursement

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    private String notes; // Any additional notes

    @PrePersist
    protected void onCreate() {
        preparedAt = LocalDateTime.now();
        if (disbursementNumber == null) {
            disbursementNumber = "DISB-" + System.currentTimeMillis();
        }
    }

    public enum DisbursementMethod {
        CHEQUE,         // Physical cheque
        BANK_TRANSFER,  // Direct bank transfer
        MPESA,          // M-Pesa mobile money
        CASH,           // Cash payment
        RTGS,           // Real-time gross settlement
        EFT             // Electronic funds transfer
    }

    public enum DisbursementStatus {
        PENDING_PREPARATION,  // Waiting for treasurer to prepare
        PREPARED,             // Treasurer has prepared (cheque written/transfer form ready)
        PENDING_APPROVAL,     // Waiting for final approval
        APPROVED,             // Approved, ready to disburse
        DISBURSED,           // Money sent/given to member
        COLLECTED,           // Member collected cheque/cash
        CLEARED,             // Cheque cleared / Transfer successful
        BOUNCED,             // Cheque bounced
        CANCELLED            // Disbursement cancelled
    }
}

