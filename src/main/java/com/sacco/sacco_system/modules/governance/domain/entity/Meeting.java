package com.sacco.sacco_system.modules.governance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a committee meeting
 */
@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String meetingNumber;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingType meetingType;

    @Column(nullable = false)
    private LocalDate meetingDate;

    @Column(nullable = false)
    private LocalTime meetingTime;

    @Column(nullable = false)
    private String venue;

    @Lob
    private String agenda;

    @Lob
    private String minutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    private String chairperson;
    private String secretary;

    @ElementCollection
    @CollectionTable(name = "meeting_attendees", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "attendee")
    @Builder.Default
    private List<String> attendees = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;

    public enum MeetingType {
        LOAN_COMMITTEE,
        BOARD_MEETING,
        ANNUAL_GENERAL_MEETING,
        SPECIAL_MEETING,
        EMERGENCY_MEETING
    }

    public enum MeetingStatus {
        SCHEDULED,           // Meeting scheduled, not started
        IN_PROGRESS,         // Voting is open
        VOTING_CLOSED,       // Chairperson closed voting, awaiting secretary to finalize
        COMPLETED,           // Secretary finalized results and generated minutes
        CANCELLED,
        POSTPONED
    }
}

