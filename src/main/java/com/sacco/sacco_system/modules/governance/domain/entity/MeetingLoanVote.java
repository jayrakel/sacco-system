package com.sacco.sacco_system.modules.governance.domain.entity;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a committee member's vote on a loan agenda item
 */
@Entity
@Table(name = "meeting_loan_votes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_loan_agenda_id", "member_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingLoanVote {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_loan_agenda_id", nullable = false)
    private MeetingLoanAgenda agendaItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member voter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteDecision decision;

    @Lob
    private String comments;

    @CreationTimestamp
    private LocalDateTime votedAt;

    public enum VoteDecision {
        APPROVE,
        REJECT,
        ABSTAIN,
        DEFER
    }
}

