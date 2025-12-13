package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    
    private LocalDate reportDate;
    
    private BigDecimal totalMembers = BigDecimal.ZERO;
    
    private BigDecimal totalSavings = BigDecimal.ZERO;
    
    private BigDecimal totalLoansIssued = BigDecimal.ZERO;
    
    private BigDecimal totalLoansOutstanding = BigDecimal.ZERO;
    
    private BigDecimal totalRepayments = BigDecimal.ZERO;
    
    private BigDecimal totalInterestCollected = BigDecimal.ZERO;
    
    private BigDecimal totalShareCapital = BigDecimal.ZERO;
    
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;
    
    private BigDecimal netIncome = BigDecimal.ZERO;
    
    private LocalDateTime generatedAt;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
        createdAt = LocalDateTime.now();
    }
}
