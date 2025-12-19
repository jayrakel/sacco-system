package com.sacco.sacco_system.modules.admin.domain.entity;
import com.sacco.sacco_system.modules.admin.domain.entity.AuditLog;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String username;

    private String action;

    private String entityName;

    private String entityId;

    // âœ… FIX: Use TEXT to allow long descriptions (JSON payloads etc.)
    @Column(columnDefinition = "TEXT")
    private String details;

    private String ipAddress;

    @CreationTimestamp
    private LocalDateTime timestamp;
}



