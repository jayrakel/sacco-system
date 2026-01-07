package com.sacco.sacco_system.modules.member.api.dto;

import com.sacco.sacco_system.modules.member.domain.entity.Member.Gender;
import com.sacco.sacco_system.modules.member.domain.entity.Member.MaritalStatus;
import com.sacco.sacco_system.modules.member.domain.entity.Member.MemberStatus;
import com.sacco.sacco_system.modules.member.domain.entity.RegistrationStatus; // Assuming Enum location based on package structure
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDTO {

    // Identity
    private UUID id;
    private UUID userId; // Link to Auth User

    // Business Identifiers (Dictionary Section 3)
    private String memberNumber;
    private String memberId; // Added per Dictionary

    // Personal Details (Dictionary Section 3)
    private String firstName;
    private String middleName; // Retained (Flagged: Not in Dictionary)
    private String lastName;

    // Renamed from idNumber -> nationalId (Dictionary Section 3)
    private String nationalId;

    private String kraPin;

    private LocalDate dateOfBirth;
    private Gender gender; // Retained (Flagged: Not in Dictionary)
    private MaritalStatus maritalStatus; // Retained (Flagged: Not in Dictionary)

    // Contact (Dictionary Section 3)
    private String phoneNumber;
    private String email;

    // Renamed from physicalAddress -> address (Dictionary Section 3)
    private String address;

    private String profileImageUrl;

    // Status & Metadata (Dictionary Section 3)
    private LocalDateTime membershipDate;
    private MemberStatus memberStatus;

    // Added per Dictionary Section 3
    private RegistrationStatus registrationStatus;
}