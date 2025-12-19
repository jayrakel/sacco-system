package com.sacco.sacco_system.modules.savings.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import com.sacco.sacco_system.modules.savings.api.dto.SavingsAccountDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsAccountDTO {
    private UUID id;
    private String accountNumber;
    private UUID memberId;
    private String memberName;
    private BigDecimal balance;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private String status;

    // âœ… NEW FIELDS FOR UI
    private String productName;
    private BigDecimal interestRate;
    private LocalDate maturityDate;
}



