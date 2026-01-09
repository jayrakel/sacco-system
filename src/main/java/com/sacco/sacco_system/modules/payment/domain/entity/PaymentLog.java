package com.sacco.sacco_system.modules.payment.domain.entity;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // The Unique ID from Safaricom (Used for tracking)
    @Column(unique = true, nullable = false)
    private String checkoutRequestId;

    private String phoneNumber;
    private BigDecimal amount;

    // e.g., "LOAN_APPLICATION_FEE", "SAVINGS_DEPOSIT"
    private String transactionType;

    // Reference to the domain entity being paid for (e.g. Loan ID)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String resultDescription; // e.g., "Insufficient Funds"

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = PaymentStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}