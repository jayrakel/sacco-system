package com.sacco.sacco_system.modules.notification.domain.service;

import com.sacco.sacco_system.modules.notification.domain.entity.Notification;
import com.sacco.sacco_system.modules.notification.domain.entity.NotificationType;
import com.sacco.sacco_system.modules.notification.domain.repository.NotificationRepository;
import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * General purpose notification with optional Email/SMS channels.
     */
    @Transactional
    public void notifyUser(UUID userId, String title, String message, boolean sendEmail, boolean sendSms) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Attempted to notify non-existent user ID: {}", userId);
            return;
        }

        // 1. In-App Notification (Reuse core method)
        createNotification(user, title, message, Notification.NotificationType.INFO);

        // 2. Email
        if (sendEmail) {
            log.info(">> ðŸ“§ Email queued for: {}", user.getEmail());
            // emailService.sendGenericEmail(user.getEmail(), title, message);
        }

        // 3. SMS
        if (sendSms) {
            log.info(">> ðŸ“± SMS queued for user ID: {}", userId);
        }
    }

    /**
     * Core method to save notification to DB.
     * Used by LoanService and other internal services.
     */
    public void createNotification(User user, String title, String message, NotificationType type) {
        // TODO: NotificationType parameter type doesn't match - accepting but using INFO type
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(Notification.NotificationType.INFO)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(UUID userId) {
        // TODO: Repository method doesn't exist - returning empty list for now
        return new java.util.ArrayList(); // notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            // TODO: setRead method doesn't exist - using isRead instead
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    public long getUnreadCount(UUID userId) {
        // TODO: Repository method doesn't exist - returning 0 for now
        return 0; // notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}

