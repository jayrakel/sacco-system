package com.sacco.sacco_system.modules.member.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDTO {
    private UUID id;
    private String profileImageUrl;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String nationalId;
    private String kraPin;
    private String address;
    private LocalDate dateOfBirth;
    
    // Old simple Next of Kin (Keep for backward compatibility if needed, or deprecate)
    private String nextOfKinName;
    private String nextOfKinPhone;
    private String nextOfKinRelation;

    private String memberStatus;  // ✅ Changed from 'status' to match dictionary
    private BigDecimal totalShares;
    private BigDecimal totalSavings;
    private LocalDateTime membershipDate;

    // ✅ NEW FIELDS
    private List<BeneficiaryDTO> beneficiaries;
    private EmploymentDetailsDTO employmentDetails;
}