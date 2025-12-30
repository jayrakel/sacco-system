package com.sacco.sacco_system.modules.loan.api.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder
public class LoanResponseDTO {
    private UUID id;
    private String loanNumber;
    private String productName;
    private BigDecimal principalAmount;

    // ✅ ADDED: Current Balance
    private BigDecimal balance;

    private String status;
    private LocalDate applicationDate;
}