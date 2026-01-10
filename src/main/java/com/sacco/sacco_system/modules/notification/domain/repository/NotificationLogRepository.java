package com.sacco.sacco_system.modules.notification.domain.repository;

import com.sacco.sacco_system.modules.notification.domain.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
}