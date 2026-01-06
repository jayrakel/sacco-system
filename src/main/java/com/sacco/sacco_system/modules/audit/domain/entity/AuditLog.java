package com.sacco.sacco_system.modules.audit.domain.entity;

import jakarta.persistence.*;
import lombok.*;
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

    // Target Entity Context (e.g., "Loan", "Member")
    @Column(nullable = false, updatable = false)
    private String entityName; // Renamed from entity

    // Renamed from recordId -> entityId
    @Column(nullable = false, updatable = false)
    private UUID entityId;

    // Action Performed
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditAction action; // Renamed from type

    // Security Context: Who & Where
    // Renamed from userId -> actorId
    @Column(nullable = false, updatable = false)
    private UUID actorId;

    // Renamed from ip -> ipAddress
    @Column(name = "ip_address", updatable = false)
    private String ipAddress;

    // Evidence: What changed (JSON Snapshot)
    // Renamed from details -> changes
    @Column(columnDefinition = "TEXT", updatable = false)
    private String changes;

    // Timestamp (Renamed from date -> occurredAt)
    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    public enum AuditAction {
        CREATE,
        UPDATE,
        DELETE,
        APPROVE,
        REJECT,
        LOGIN,
        LOGOUT,
        EXPORT_DATA
    }
}