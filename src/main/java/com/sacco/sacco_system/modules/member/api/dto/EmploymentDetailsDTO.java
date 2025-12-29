package com.sacco.sacco_system.modules.member.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmploymentDetailsDTO {
    private String terms; // e.g., PERMANENT
    private String employerName;
    private String staffNumber;
    private String stationOrDepartment;
    private LocalDate dateEmployed;
    private LocalDate contractExpiryDate;
    
    // Financials for Logic
    private BigDecimal grossMonthlyIncome;
    private BigDecimal netMonthlyIncome;
    
    // Banking
    private String bankName;
    private String bankBranch;
    private String bankAccountNumber;
}