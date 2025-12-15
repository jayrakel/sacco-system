package com.sacco.sacco_system.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class LoanAgingDTO {
    private String loanNumber;
    private String memberName;
    private BigDecimal amountOutstanding;
    private int daysOverdue;
    private String category; // e.g., "30-60 Days", "90+ Days"
}