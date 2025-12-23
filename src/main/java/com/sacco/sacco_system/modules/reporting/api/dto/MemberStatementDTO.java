package com.sacco.sacco_system.modules.reporting.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberStatementDTO {
    // Organization Branding (Dynamic)
    private String organizationName;
    private String organizationAddress;
    private String organizationEmail;
    private String organizationLogoUrl; // NEW: Holds the URL/Base64 of the logo

    // Member & Statement Metadata
    private String memberName;
    private String memberNumber;
    private String memberAddress;
    private String statementReference;
    private LocalDate generatedDate;
    
    // Financial Summary
    private BigDecimal openingBalance;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal closingBalance;

    // The Transactions
    private List<StatementTransaction> transactions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatementTransaction {
        private LocalDate date;
        private String reference;
        private String description;
        private String type;
        private BigDecimal amount;
        private BigDecimal runningBalance;
    }
}