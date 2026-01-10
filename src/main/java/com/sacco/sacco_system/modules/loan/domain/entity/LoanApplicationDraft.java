package com.sacco.sacco_system.modules.loan.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_application_drafts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanApplicationDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnore // ✅ CRITICAL FIX: Stops the 500 Error (Circular Dependency)
    private Member member;

    @Column(unique = true, nullable = false)
    private String draftReference;

    @Column(nullable = false)
    @Builder.Default
    private boolean feePaid = false;

    // Optional fields (User selection)
    private UUID selectedProductId;
    private BigDecimal intendedAmount;
    private Integer intendedDurationWeeks;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DraftStatus status = DraftStatus.PENDING_FEE;

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

    public enum DraftStatus {
        PENDING_FEE,
        FEE_PAID,
        CONVERTED,
        ABANDONED
    }
}