package com.sacco.sacco_system.modules.members.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class MemberStatementDTO {
    private LocalDate date;
    private String reference;
    private String description;
    private String type;
    private BigDecimal amount;
    private BigDecimal runningBalance;
}