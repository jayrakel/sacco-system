package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @Column(name = "setting_key", unique = true, nullable = false)
    private String key; // e.g., "REGISTRATION_FEE"

    @Column(name = "setting_value", nullable = false)
    private String value; // e.g., "1000"

    private String description; // e.g., "Fee charged for new member registration"

    private String dataType; // "STRING", "NUMBER", "BOOLEAN"
}