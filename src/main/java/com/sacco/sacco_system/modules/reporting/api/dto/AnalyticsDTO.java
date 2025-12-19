package com.sacco.sacco_system.modules.reporting.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsDTO {
    private String metricName;
    private BigDecimal value;
    private String unit;
    private String trend;
}
