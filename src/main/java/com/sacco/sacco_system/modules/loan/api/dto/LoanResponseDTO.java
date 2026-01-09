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

    // ✅ Changed from 'balance' to match dictionary
    private BigDecimal totalOutstandingAmount;

    // ✅ Changed from 'status' to match dictionary
    private String loanStatus;
    private LocalDate applicationDate;

    // ✅ ADDED: Fixes "cannot find symbol" error
    private boolean feePaid;
}