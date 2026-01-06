package com.sacco.sacco_system.modules.admin.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Unique Identifier (e.g., "LOAN_INTEREST_RATE_DEFAULT")
    @Column(nullable = false, unique = true)
    private String configKey; // Renamed from key

    @Column(nullable = false)
    private String configValue; // Renamed from value

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataType dataType; // UI Hint (STRING, BOOLEAN, etc.)

    // --- Global Audit ---
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SettingCategory {
        GENERAL,
        FINANCE,
        LOAN,
        NOTIFICATION,
        SECURITY
    }

    public enum DataType {
        STRING,
        INTEGER,
        DECIMAL,
        BOOLEAN
    }
}