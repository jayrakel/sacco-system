package com.sacco.sacco_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsAccountDTO {
    private Long id;
    private String accountNumber;
    private Long memberId;
    private String memberName;
    private BigDecimal balance;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private String status;
}
