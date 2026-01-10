package com.sacco.sacco_system.modules.loan.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_guarantors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "loan"})
public class Guarantor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private BigDecimal guaranteedAmount;

    @Enumerated(EnumType.STRING)
    private GuarantorStatus status;

    // Global Audit & Identity fields (Phase A requirement)
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

    public enum GuarantorStatus { PENDING, ACCEPTED, DECLINED }
}