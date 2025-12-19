package com.sacco.sacco_system.modules.notifications.repository;

import com.sacco.sacco_system.modules.notifications.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Notification> findByUserIdAndIsReadFalse(UUID userId);

    long countByUserIdAndIsReadFalse(UUID userId);
}