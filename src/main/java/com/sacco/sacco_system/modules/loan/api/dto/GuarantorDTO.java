package com.sacco.sacco_system.modules.loan.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuarantorDTO {
    private UUID id;
    private UUID memberId;
    private String memberName;

    // ✅ ADDED: Required for Dashboard Requests View
    private UUID loanId;
    private String loanNumber;
    private String applicantName;

    // Contact/Personal Info
    private String name;
    private String email;
    private String phone;
    private String relationship;
    private String idNumber;

    private BigDecimal guaranteeAmount;
    private String status;
}