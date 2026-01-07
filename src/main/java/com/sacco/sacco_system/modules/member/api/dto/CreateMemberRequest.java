package com.sacco.sacco_system.modules.member.api.dto;

import com.sacco.sacco_system.modules.member.domain.entity.Member.Gender;
import com.sacco.sacco_system.modules.member.domain.entity.Member.MaritalStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating a new member
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMemberRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName; // Retained (Flagged: Not in Dictionary)

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "National ID is required")
    // Renamed from idNumber -> nationalId (Dictionary Section 3)
    private String nationalId;

    private String kraPin;

    // Renamed from physicalAddress -> address (Dictionary Section 3)
    private String address;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender; // Retained (Flagged: Not in Dictionary)

    @NotNull(message = "Marital status is required")
    private MaritalStatus maritalStatus; // Retained (Flagged: Not in Dictionary)
}