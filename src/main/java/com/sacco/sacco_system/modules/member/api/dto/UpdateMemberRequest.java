package com.sacco.sacco_system.modules.member.api.dto;

import com.sacco.sacco_system.modules.member.domain.entity.Member.Gender;
import com.sacco.sacco_system.modules.member.domain.entity.Member.MaritalStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating a member
 * Note: Immutable fields (idNumber, memberNumber) are excluded.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMemberRequest {

    private String firstName;

    private String middleName; // Added Phase 3

    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    // Renamed from phone -> phoneNumber
    @Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "Phone number should be valid")
    private String phoneNumber;

    // Renamed from address -> physicalAddress
    private String physicalAddress;

    private String profileImageUrl; // Added Phase 3

    private String kraPin; // Added Phase 3

    private LocalDate dateOfBirth; // Editable in case of correction

    private Gender gender; // Added Phase 3

    private MaritalStatus maritalStatus; // Added Phase 3
}