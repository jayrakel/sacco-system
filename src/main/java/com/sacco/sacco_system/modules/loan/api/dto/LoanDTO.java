package com.sacco.sacco_system.modules.loan.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDTO {
    private UUID id;
    private String loanNumber;
    private UUID memberId;
    private String memberName;
    private String productName;
    private BigDecimal principalAmount;
    private BigDecimal loanBalance;
    private Integer duration;
    private String durationUnit;
    private String status;
    private LocalDate applicationDate;
    private LocalDate disbursementDate;

    // ✅ ADDED: This resolves the builder compilation error
    private String expectedRepaymentDate;

    private BigDecimal weeklyRepaymentAmount;

    // Fee & Voting info
    private boolean applicationFeePaid;
    private BigDecimal processingFee;
    private Integer votesYes;
    private Integer votesNo;
}