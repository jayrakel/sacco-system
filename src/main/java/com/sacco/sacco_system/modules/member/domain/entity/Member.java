package com.sacco.sacco_system.modules.member.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    // 1. Global Definition: Audit & Identity
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // 2. Business Identifier
    // Enforced via @PrePersist
    @Column(unique = true, nullable = false)
    private String memberId;

    // 3. System Linkage (Loose Coupling)
    @Column(name = "user_id")
    private UUID userId;

    // 4. Personal Details
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "National ID is required")
    @Column(unique = true, nullable = false)
    private String nationalId;

    @Column(unique = true)
    private String kraPin;

    private LocalDate dateOfBirth;

    // 5. Contact Details
    @Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "Phone number should be valid")
    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Email(message = "Email should be valid")
    @Column(unique = true, nullable = false)
    private String email;

    private String address;

    // 6. Membership Details
    @NotBlank(message = "Member number is required")
    @Column(unique = true, nullable = false)
    private String memberNumber;

    private LocalDateTime membershipDate;

    private String profileImageUrl;

    // 7. Status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MemberStatus memberStatus = MemberStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RegistrationStatus registrationStatus = RegistrationStatus.PENDING;

    // 8. Relationships
    // OPTION A FIX: Unidirectional One-to-Many using @JoinColumn
    // Member owns the relationship and manages the 'member_id' FK in the beneficiaries table.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @Builder.Default
    private List<Beneficiary> beneficiaries = new ArrayList<>();

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private EmploymentDetails employmentDetails;

    // Helper Methods
    public void addBeneficiary(Beneficiary beneficiary) {
        beneficiaries.add(beneficiary);
        // No need to set 'member' on beneficiary as it's unidirectional loose coupling
    }

    public void setEmploymentDetails(EmploymentDetails details) {
        this.employmentDetails = details;
        if (details != null) {
            details.setMember(this);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (memberId == null) {
            memberId = UUID.randomUUID().toString();
        }
        if (membershipDate == null) {
            membershipDate = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (memberStatus == null) {
            memberStatus = MemberStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MemberStatus {
        ACTIVE,
        SUSPENDED,
        EXITED,
        DECEASED
    }

    public enum RegistrationStatus {
        PENDING,
        PAID
    }
}