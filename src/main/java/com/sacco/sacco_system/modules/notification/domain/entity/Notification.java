package com.sacco.sacco_system.modules.notification.domain.entity;

import com.sacco.sacco_system.modules.auth.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String title;
    private String message;
    private String recipientEmail;
    private String recipientPhone;
    private String subject;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String failureReason;
    private Integer retryCount = 0;
}
