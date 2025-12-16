package com.sacco.sacco_system.service;

import com.sacco.sacco_system.entity.Notification;
import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.repository.NotificationRepository;
import com.sacco.sacco_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ✅ Import
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j // ✅ Annotation
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void notifyUser(UUID userId, String title, String message, boolean sendEmail, boolean sendSms) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        // 1. In-App Notification
        Notification notif = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(Notification.NotificationType.INFO)
                .build();
        notificationRepository.save(notif);

        // 2. Email
        if (sendEmail) {
            log.info(">> 📧 Email queued for: {}", user.getEmail()); // ✅ Replaced Sysout
            // emailService.sendGenericEmail(user.getEmail(), title, message);
        }

        // 3. SMS
        if (sendSms) {
            log.info(">> 📱 SMS queued for user ID: {}", userId); // ✅ Replaced Sysout
        }
    }

    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}