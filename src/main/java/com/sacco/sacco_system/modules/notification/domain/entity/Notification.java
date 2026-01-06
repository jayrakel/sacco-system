package com.sacco.sacco_system.modules.notification.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Target Recipient (Loose Coupling)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Polymorphic Source (What triggered this?)
    @Column(name = "reference_id")
    private UUID referenceId; // e.g., Loan ID or Transaction ID

    @Column(name = "reference_type")
    private String referenceType; // e.g., "LOAN", "TRANSACTION"

    // --- Message Details ---

    @Column(nullable = false)
    private String recipient; // stored snapshot (email address or phone)

    private String subject; // Optional for SMS

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    // --- Delivery Status ---

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationStatus notificationStatus = NotificationStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    private String failureReason; // Debugging info

    // --- Global Audit ---
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy; // e.g., "SYSTEM"
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (notificationStatus == null) notificationStatus = NotificationStatus.PENDING;
        if (retryCount == null) retryCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum NotificationType {
        EMAIL,
        SMS,
        IN_APP
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        RETRYING
    }
}