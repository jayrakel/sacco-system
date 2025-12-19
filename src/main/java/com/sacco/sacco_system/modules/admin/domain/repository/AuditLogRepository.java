package com.sacco.sacco_system.modules.admin.domain.repository;

import com.sacco.sacco_system.modules.admin.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import com.sacco.sacco_system.modules.admin.domain.repository.AuditLogRepository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByUsername(String username);
    List<AuditLog> findByEntityName(String entityName);
    // You can add filtering by date range here later
}



