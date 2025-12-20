package com.sacco.sacco_system.modules.admin.domain.entity;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meeting_agendas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(nullable = false)
    private Integer agendaNumber; // Order in the meeting (1, 2, 3...)

    @Column(nullable = false)
    private String agendaTitle; // e.g., "Loan Application - John Doe"

    private String agendaDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgendaType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgendaStatus status;

    // Link to loan (if agenda is about loan approval)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    // Voting results
    private Integer votesYes;
    private Integer votesNo;
    private Integer votesAbstain;

    @Enumerated(EnumType.STRING)
    private AgendaDecision decision; // APPROVED, REJECTED, DEFERRED

    private String decisionNotes; // Secretary's notes on the decision

    @Column(name = "tabled_at")
    private LocalDateTime tabledAt;

    @Column(name = "tabled_by")
    private UUID tabledBy; // User ID (usually Secretary)

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @Column(name = "finalized_by")
    private UUID finalizedBy; // User ID (usually Secretary)

    @PrePersist
    protected void onCreate() {
        tabledAt = LocalDateTime.now();
        votesYes = 0;
        votesNo = 0;
        votesAbstain = 0;
    }

    public enum AgendaType {
        LOAN_APPROVAL,        // Vote on loan application
        POLICY_CHANGE,        // Vote on policy updates
        BUDGET_APPROVAL,      // Vote on budget
        MEMBER_ADMISSION,     // Vote on new member
        OFFICER_ELECTION,     // Vote for officials
        GENERAL_DISCUSSION,   // Discussion item (no vote needed)
        OTHER                 // Other agenda items
    }

    public enum AgendaStatus {
        TABLED,        // Added to meeting agenda
        OPEN_FOR_VOTE, // Voting is open
        VOTING_CLOSED, // Voting closed, awaiting finalization
        FINALIZED      // Decision made and recorded
    }

    public enum AgendaDecision {
        APPROVED,  // Majority voted yes
        REJECTED,  // Majority voted no
        DEFERRED,  // Not enough votes or postponed
        TIE        // Equal yes/no votes
    }
}

