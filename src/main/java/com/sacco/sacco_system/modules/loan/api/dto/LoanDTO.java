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
    private BigDecimal interestRate;
    private Integer duration;
    private String durationUnit;
    private BigDecimal loanBalance;
    private String status;
    private LocalDate applicationDate;
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private BigDecimal processingFee;
    private BigDecimal memberSavings;
    private BigDecimal weeklyRepaymentAmount;

    // âœ… NEW: Missing fields for Voting
    private int votesYes;
    private int votesNo;
}



