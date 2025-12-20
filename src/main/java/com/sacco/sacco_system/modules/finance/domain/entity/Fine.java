package com.sacco.sacco_system.modules.finance.domain.entity;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fine Entity
 * Represents penalties for late payments, missed meetings, or other infractions
 */
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

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;  // Optional - if fine is related to a loan

    @Enumerated(EnumType.STRING)
    private FineType type;

    private BigDecimal amount;

    private String description;

    private LocalDate fineDate;

    @Enumerated(EnumType.STRING)
    private FineStatus status = FineStatus.PENDING;

    private LocalDate paymentDate;

    private String paymentReference;

    private Integer daysOverdue;  // For late payment fines

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum FineType {
        LATE_LOAN_PAYMENT,      // Late loan repayment
        MISSED_MEETING,         // Member missed mandatory meeting
        LOAN_DEFAULT,           // Loan default penalty
        ADMINISTRATIVE,         // General administrative fine
        OTHER                   // Other penalties
    }

    public enum FineStatus {
        PENDING,    // Fine imposed but not paid
        PAID,       // Fine paid
        WAIVED      // Fine waived/forgiven
    }
}

