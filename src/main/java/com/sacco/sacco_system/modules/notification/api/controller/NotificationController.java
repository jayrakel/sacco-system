package com.sacco.sacco_system.modules.notification.api.controller;

import com.sacco.sacco_system.modules.notification.domain.entity.Notification;
import com.sacco.sacco_system.modules.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notification Controller
 * Manages in-app notifications for users
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get user's notifications
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserNotifications(@PathVariable UUID userId) {
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        long unreadCount = notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "notifications", notifications,
                        "unreadCount", unreadCount
                )
        ));
    }

    /**
     * Get unread count for a user
     */
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable UUID userId) {
        long count = notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("unreadCount", count)
        ));
    }

    /**
     * Mark a notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable UUID id) {
        try {
            notificationService.markAsRead(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notification marked as read"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable UUID userId) {
        try {
            notificationService.markAllAsRead(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "All notifications marked as read"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Send a custom notification to a user
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody Map<String, Object> request) {
        try {
            UUID userId = UUID.fromString((String) request.get("userId"));
            String title = (String) request.get("title");
            String message = (String) request.get("message");
            boolean sendEmail = request.containsKey("sendEmail") && (boolean) request.get("sendEmail");
            boolean sendSms = request.containsKey("sendSms") && (boolean) request.get("sendSms");

            notificationService.notifyUser(userId, title, message, sendEmail, sendSms);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notification sent successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}

