package com.sacco.sacco_system.modules.notification.domain.repository;

import com.sacco.sacco_system.modules.notification.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByStatus(String status);
    List<Notification> findByRecipientEmail(String email);
}
