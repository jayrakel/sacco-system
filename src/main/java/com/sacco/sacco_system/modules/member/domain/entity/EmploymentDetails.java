package com.sacco.sacco_system.modules.member.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employment_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmploymentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Structural: Decoupled Member (One-to-One context)
    @Column(name = "member_id", nullable = false, unique = true)
    private UUID memberId;

    @Column(nullable = false)
    private String employerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType; // Renamed from terms

    private String staffNumber; // Critical for Check-off

    private String department; // Renamed from stationOrDepartment

    private LocalDate dateEmployed;

    private LocalDate contractExpiryDate; // Nullable (for CONTRACT type)

    // Financials for Loan Eligibility (Debt Ratio)
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal grossMonthlyIncome = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal netMonthlyIncome = BigDecimal.ZERO;

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
        if (employmentType == null) employmentType = EmploymentType.PERMANENT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum EmploymentType {
        PERMANENT,
        CONTRACT,
        SELF_EMPLOYED,
        UNEMPLOYED,
        RETIRED
    }
}