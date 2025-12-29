package com.sacco.sacco_system.modules.notification.domain.service;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository; // ✅ Added
import com.sacco.sacco_system.modules.notification.domain.entity.Notification;
import com.sacco.sacco_system.modules.notification.domain.repository.NotificationRepository;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;

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
    private final MemberRepository memberRepository; // ✅ Injected to resolve Member IDs
    private final EmailService emailService;

    /**
     * Smart Notification: Handles both User IDs and Member IDs.
     */
    @Transactional
    public void notifyUser(UUID targetId, String title, String message, boolean sendEmail, boolean sendSms) {
        User user = resolveUser(targetId);

        if (user == null) {
            log.warn("❌ Failed to notify: Target ID {} not found in User or Member records.", targetId);
            return;
        }

        // 1. In-App Notification
        createNotification(user, title, message, Notification.NotificationType.INFO);

        // 2. Email Notification
        if (sendEmail) {
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                log.info(">> 📧 Sending email to: {}", user.getEmail());
                try {
                    emailService.sendGenericEmail(user.getEmail(), title, message);
                } catch (Exception e) {
                    log.error("❌ Failed to send email to {}: {}", user.getEmail(), e.getMessage());
                }
            } else {
                log.warn("⚠️ Cannot send email: User {} has no email address.", user.getUsername());
            }
        }

        // 3. SMS Notification (Placeholder)
        if (sendSms) {
            if (user.getPhoneNumber() != null) {
                // log.info(">> 📱 SMS queued for: {}", user.getPhoneNumber());
                // smsService.sendSms(user.getPhoneNumber(), message);
            }
        }
    }

    @Transactional
    public void notifyAll(String title, String message) {
        List<User> allUsers = userRepository.findAll();
        allUsers.forEach(user -> {
            try {
                createNotification(user, title, message, Notification.NotificationType.INFO);
            } catch (Exception e) {
                log.error("Failed to notify user {}: {}", user.getId(), e.getMessage());
            }
        });
        log.info("📢 Broadcast notification sent to {} users.", allUsers.size());
    }

    /**
     * Helper to resolve the real User entity from a UUID that could be a UserID OR a MemberID.
     */
    private User resolveUser(UUID targetId) {
        // 1. Try finding as User ID (System Admin / Direct User)
        return userRepository.findById(targetId)
                .or(() -> {
                    // 2. Fallback: Try finding as Member ID (Guarantor / Loan Applicant)
                    return memberRepository.findById(targetId).map(Member::getUser);
                })
                .orElse(null);
    }

    /**
     * Core method to save notification to DB.
     */
    public void createNotification(User user, String title, String message, Notification.NotificationType type) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(UUID userId) {
        // Ensure we are querying by USER ID. If frontend passes Member ID, resolve it.
        User user = resolveUser(userId);
        if (user == null) return List.of();

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public void markAsRead(UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public long getUnreadCount(UUID userId) {
        User user = resolveUser(userId);
        if (user == null) return 0;
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    public void markAllAsRead(UUID userId) {
        User user = resolveUser(userId);
        if (user == null) return;

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        notifications.forEach(n -> {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }
}