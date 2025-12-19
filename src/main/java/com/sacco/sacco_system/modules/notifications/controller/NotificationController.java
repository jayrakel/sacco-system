package com.sacco.sacco_system.modules.notifications.controller;

import com.sacco.sacco_system.modules.notifications.model.Notification;
import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.notifications.service.NotificationService;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyNotifications() {
        User user = getAuthenticatedUser();
        List<Notification> list = notificationService.getUserNotifications(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}