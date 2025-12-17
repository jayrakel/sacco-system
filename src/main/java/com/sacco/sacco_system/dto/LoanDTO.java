package com.sacco.sacco_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDTO {
    private UUID id;
    private String loanNumber;
    private UUID memberId;
    private String memberName;
    private BigDecimal principalAmount;
    private BigDecimal loanBalance;
    private BigDecimal interestRate;
    private BigDecimal totalInterest;
    private Integer durationMonths;
    private BigDecimal monthlyRepayment;
    private String status;
    private LocalDate applicationDate;
    private LocalDate submissionDate;
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private LocalDate expectedRepaymentDate;
    private String productName;
    private BigDecimal processingFee;
    private BigDecimal memberSavings;
}
