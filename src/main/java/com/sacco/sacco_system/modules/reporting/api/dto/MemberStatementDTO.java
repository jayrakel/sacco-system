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
public class MemberStatementDTO {
    private String memberId;
    private String memberName;
    private LocalDate statementDate;
    private BigDecimal totalSavings;
    private BigDecimal totalLoans;
    private BigDecimal netPosition;
    private String accountStatus;
}
