package com.sacco.sacco_system.modules.admin.domain.entity;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agenda_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"agenda_id", "member_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agenda_id", nullable = false)
    private MeetingAgenda agenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteChoice vote;

    @Column(name = "voted_at")
    private LocalDateTime votedAt;

    private String comments; // Optional: Member can explain their vote

    @PrePersist
    protected void onCreate() {
        votedAt = LocalDateTime.now();
    }

    public enum VoteChoice {
        YES,      // In favor
        NO,       // Against
        ABSTAIN   // No opinion / conflict of interest
    }
}

