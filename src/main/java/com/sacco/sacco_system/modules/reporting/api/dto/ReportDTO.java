package com.sacco.sacco_system.modules.reporting.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDTO {
    private String reportName;
    private LocalDate reportDate;
    private BigDecimal totalAmount;
    private String status;
}
