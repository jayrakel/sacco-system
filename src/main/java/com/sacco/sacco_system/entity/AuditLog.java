package com.sacco.sacco_system.entity;

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

    private String username; // The user who performed the action

    private String action; // e.g., "APPROVE_LOAN", "UPDATE_SETTINGS"

    private String entityName; // e.g., "Loan", "Member"

    private String entityId; // The ID of the item affected

    @Column(length = 1000)
    private String details; // e.g., "Changed Interest Rate from 12% to 14%"

    private String ipAddress;

    @CreationTimestamp
    private LocalDateTime timestamp;
}