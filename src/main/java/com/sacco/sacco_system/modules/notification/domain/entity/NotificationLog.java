package com.sacco.sacco_system.modules.notification.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String templateCode; // e.g., "LOAN_STATUS_UPDATE", "GENERIC_EMAIL"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String recipient; // Email address or Phone number

    private String referenceType; // e.g., "LOAN", "MEMBER"
    private String referenceId;   // UUID of the entity

    @Enumerated(EnumType.STRING)
    private NotificationStatus status; // SENT, FAILED

    private LocalDateTime sentAt;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    // --- Global Audit Fields (as per Dictionary) ---
    @Builder.Default
    private Boolean active = true;
    private LocalDateTime createdAt;
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.createdBy = "SYSTEM";
        if (this.sentAt == null) this.sentAt = LocalDateTime.now();
    }

    public enum NotificationChannel {
        EMAIL, SMS, IN_APP
    }

    public enum NotificationStatus {
        SENT, FAILED, RETRIED
    }
}