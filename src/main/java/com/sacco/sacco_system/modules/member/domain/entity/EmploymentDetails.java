package com.sacco.sacco_system.modules.member.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private EmploymentTerms terms; // PERMANENT, CONTRACT, CASUAL, SELF_EMPLOYED

    private String employerName;
    private String staffNumber;
    private String stationOrDepartment; // e.g., "Headquarters" or "HR Dept"

    private LocalDate dateEmployed;
    private LocalDate contractExpiryDate; // Nullable if Permanent

    // 🧠 BRAIN CONNECTION: Used for "1/3rd Rule" Calculation
    private BigDecimal grossMonthlyIncome;
    private BigDecimal netMonthlyIncome;
    
    // Bank Details for Salary Processing
    private String bankName;
    private String bankBranch;
    private String bankAccountNumber;

    public enum EmploymentTerms {
        PERMANENT, CONTRACT, CASUAL, SELF_EMPLOYED, RETIRED, UNEMPLOYED
    }
}