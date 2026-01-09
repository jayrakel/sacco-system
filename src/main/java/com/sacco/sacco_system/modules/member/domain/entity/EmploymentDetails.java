package com.sacco.sacco_system.modules.member.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    @JsonIgnore
    private Member member;

    @Enumerated(EnumType.STRING)
    private EmploymentTerms employmentTerms; // PERMANENT, CONTRACT, CASUAL, SELF_EMPLOYED

    private String employerName;
    private String staffNumber;
    private String stationOrDepartment; // e.g., "Headquarters" or "HR Dept"

    private LocalDate dateEmployed;
    private LocalDate contractExpiryDate; // Nullable if Permanent

    // 🧠 BRAIN CONNECTION: Used for "1/3rd Rule" Calculation
    private BigDecimal grossMonthlyIncome;
    private BigDecimal netMonthlyIncome;
    
    // Bank Details for Salary Processing (bankAccountDetails concept from dictionary)
    private String bankName;
    private String bankBranch;
    private String bankAccountNumber;

    // Global Audit & Identity fields (Phase A requirement)
    private Boolean active = true;

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

    public enum EmploymentTerms {
        PERMANENT, CONTRACT, CASUAL, SELF_EMPLOYED, RETIRED, UNEMPLOYED
    }
}