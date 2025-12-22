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
public class LoanAgingDTO {
    private String loanId;
    private String loanNumber;
    private String memberId;
    private String memberName;
    private BigDecimal outstandingBalance;
    private BigDecimal amountOutstanding;
    private Integer daysOverdue;
    private LocalDate dueDate;
    private String status;
    private String category;
}
