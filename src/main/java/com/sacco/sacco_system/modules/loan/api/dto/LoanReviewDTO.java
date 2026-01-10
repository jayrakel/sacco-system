package com.sacco.sacco_system.modules.loan.api.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Loan Officer Review - contains only necessary data without circular references
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanReviewDTO {

    // Loan Details
    private UUID id;
    private String loanNumber;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer durationWeeks;
    private BigDecimal weeklyRepaymentAmount;
    private String loanStatus;
    private LocalDate applicationDate;
    private LocalDate approvalDate;
    private BigDecimal approvedAmount;

    // Product Details
    private ProductInfo product;

    // Member Details (no circular references!)
    private MemberInfo member;

    // Guarantors
    private List<GuarantorInfo> guarantors;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProductInfo {
        private UUID id;
        private String productCode;
        private String productName;
        private BigDecimal interestRate;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MemberInfo {
        private UUID id;
        private String memberNumber;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String memberStatus;
        private String createdAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GuarantorInfo {
        private UUID id;
        private BigDecimal guaranteedAmount;
        private String status;
        private MemberInfo member;
    }
}

