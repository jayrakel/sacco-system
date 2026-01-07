package com.sacco.sacco_system.modules.member.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "beneficiaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Structural: Decoupled Member
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private String fullName; // Renamed from name

    @Column(nullable = false)
    private String relationship;

    @Column(nullable = false)
    private String idNumber; // National ID / Passport

    private String phoneNumber;

    // Benefit Allocation Percentage (e.g., 50.00 for 50%)
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal allocation = BigDecimal.ZERO;

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
}