package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.entity.Notification;
import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyNotifications() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Notification> list = notificationService.getUserNotifications(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}