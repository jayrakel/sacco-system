package com.sacco.sacco_system.modules.admin.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "meetings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String meetingNumber; // e.g., MTG-2024-001

    @Column(nullable = false)
    private String title; // e.g., "Monthly General Meeting"

    private String description;

    @Column(nullable = false)
    private LocalDate meetingDate;

    @Column(nullable = false)
    private LocalTime meetingTime;

    private String venue; // Physical location or "Online"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status;

    // Attendance tracking (for future enhancement)
    private Integer totalMembers;
    private Integer presentMembers;
    private Integer absentMembers;

    // Meeting minutes
    private String minutesNotes; // Secretary's notes

    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "scheduled_by")
    private UUID scheduledBy; // User ID of person who scheduled

    @Column(name = "opened_at")
    private LocalDateTime openedAt; // When voting was opened

    @Column(name = "closed_at")
    private LocalDateTime closedAt; // When meeting was finalized

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (meetingNumber == null) {
            meetingNumber = "MTG-" + System.currentTimeMillis();
        }
    }

    public enum MeetingType {
        GENERAL_MEETING,      // Regular monthly/quarterly meeting
        SPECIAL_MEETING,      // Called for specific purpose
        AGM,                  // Annual General Meeting
        EMERGENCY_MEETING     // Urgent matters
    }

    public enum MeetingStatus {
        SCHEDULED,     // Meeting scheduled, agendas can be added
        AGENDA_SET,    // All agendas finalized, notified members
        IN_PROGRESS,   // Meeting ongoing, voting open
        COMPLETED,     // Meeting ended, results finalized
        CANCELLED      // Meeting cancelled
    }
}

