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
    private BigDecimal totalOutstandingAmount;
    private String loanStatus;
    private LocalDate applicationDate;
    private boolean feePaid;

    private BigDecimal weeklyRepaymentAmount;
    private BigDecimal totalArrears;
    private BigDecimal totalPrepaid;

    private LocalDate nextPaymentDate;

    // ✅ ADDED: To distinguish "Grace Period" (1) from "Normal Repayment" (>1)
    private Integer nextInstallmentNumber;
}