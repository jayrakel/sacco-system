package com.sacco.sacco_system.dto;

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
public class LoanDTO {
    private Long id;
    private String loanNumber;
    private Long memberId;
    private String memberName;
    private BigDecimal principalAmount;
    private BigDecimal loanBalance;
    private BigDecimal interestRate;
    private BigDecimal totalInterest;
    private Integer durationMonths;
    private BigDecimal monthlyRepayment;
    private String status;
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private LocalDate expectedRepaymentDate;
}
