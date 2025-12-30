package com.sacco.sacco_system.modules.loan.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class LoanRequestDTO {
    private UUID productId;
    private BigDecimal amount;
    private Integer durationWeeks;
    private String paymentReference;
}