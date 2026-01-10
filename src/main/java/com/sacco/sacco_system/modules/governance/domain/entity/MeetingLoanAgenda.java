package com.sacco.sacco_system.modules.governance.domain.entity;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity linking loans to meeting agendas
 */
@Entity
@Table(name = "meeting_loan_agenda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingLoanAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private Integer agendaOrder;

    @Lob
    private String notes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AgendaStatus status = AgendaStatus.PENDING;

    @Lob
    private String discussion;

    @Lob
    private String decision;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private String createdBy;

    public enum AgendaStatus {
        PENDING,
        DISCUSSED,
        APPROVED,
        REJECTED,
        DEFERRED,
    }
}

