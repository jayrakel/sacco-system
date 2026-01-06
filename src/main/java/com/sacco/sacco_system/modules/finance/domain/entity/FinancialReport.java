package com.sacco.sacco_system.modules.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Context: Link to Fiscal Period (Optional, as some reports are ad-hoc)
    @Column(name = "fiscal_period_id")
    private UUID fiscalPeriodId;

    // Audit: Who ran the report
    @Column(name = "generated_by_user_id", nullable = false, updatable = false)
    private UUID generatedByUserId;

    @Column(nullable = false, updatable = false)
    private String reportName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ReportType reportType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    // Artifact Location
    @Column(nullable = false, updatable = false)
    private String fileUrl; // URL or Path

    // Metadata: Input parameters used (JSON or Key-Value String)
    @Column(columnDefinition = "TEXT", updatable = false)
    private String parameters;

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
        if (generatedAt == null) generatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ReportType {
        BALANCE_SHEET,
        INCOME_STATEMENT,
        TRIAL_BALANCE,
        LOAN_AGING,
        MEMBER_STATEMENT,
        CASH_FLOW
    }
}