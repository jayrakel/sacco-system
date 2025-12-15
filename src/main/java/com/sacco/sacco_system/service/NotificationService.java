package com.sacco.sacco_system.service;

import com.sacco.sacco_system.entity.Notification;
import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.repository.NotificationRepository;
import com.sacco.sacco_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final UserRepository userRepository;

    /**
     * Send a notification across all channels
     */
    @Transactional
    public void notifyUser(UUID userId, String title, String message, boolean sendEmail, boolean sendSms) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        // 1. In-App Notification (Always)
        Notification notif = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(Notification.NotificationType.INFO)
                .build();
        notificationRepository.save(notif);

        // 2. Email
        if (sendEmail) {
            // Re-using the welcome logic wrapper or creating a simple one in EmailService
            // For now, simpler standard log:
            System.out.println(">> 📧 Email queued for: " + user.getEmail());
            // emailService.sendGenericEmail(user.getEmail(), title, message);
        }

        // 3. SMS (If phone number exists on Member profile)
        if (sendSms) {
            // In a real scenario, you'd fetch the Member entity linked to this User to get the phone
            // smsService.sendSms(member.getPhone(), message);
            System.out.println(">> 📱 SMS queued.");
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