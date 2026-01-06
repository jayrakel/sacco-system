package com.sacco.sacco_system.modules.member.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "beneficiaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    // Global Definition: Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Domain Dictionary: Member Linkage (Loose Coupling)
    // OPTION A FIX: Mapped as read-only field to allow Member (Parent) to control the @JoinColumn
    // This provides access to the ID without creating a JPA writing conflict.
    @Column(name = "member_id", insertable = false, updatable = false)
    private UUID memberId;

    // Domain Dictionary: Personal Details (Split from fullName)
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String relationship;

    // Renamed: idNumber -> identityNumber
    private String identityNumber;

    // Renamed: allocation -> allocationPercentage
    @Column(name = "allocation_percentage")
    private Double allocationPercentage;

    // Global Definition: Audit & Identity
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