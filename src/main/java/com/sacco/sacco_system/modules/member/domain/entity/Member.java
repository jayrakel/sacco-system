package com.sacco.sacco_system.modules.member.domain.entity;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ✅ ADDED: Link to the authentication account to support findByUserId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    @ToString.Exclude
    private User user;

    private String profileImageUrl;

    @NotBlank(message = "Member number is required")
    @Column(unique = true, nullable = false)
    private String memberNumber;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Email should be valid")
    @Column(unique = true, nullable = false)
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "Phone number should be valid")
    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @NotBlank(message = "National ID is required")
    @Column(unique = true, nullable = false)
    private String nationalId;

    @Column(unique = true)
    private String kraPin;

    private String address;

    private LocalDate dateOfBirth;

    private String nextOfKinName;
    private String nextOfKinPhone;
    private String nextOfKinRelation;

    @Enumerated(EnumType.STRING)
    private MemberStatus memberStatus = MemberStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus registrationStatus = RegistrationStatus.PENDING;

    private BigDecimal totalShares = BigDecimal.ZERO;

    private BigDecimal totalSavings = BigDecimal.ZERO;

    // Relationships

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("member")
    @ToString.Exclude
    private List<SavingsAccount> savingsAccounts;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("member")
    @ToString.Exclude
    private List<Loan> loans;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("member")
    @ToString.Exclude
    private List<Transaction> transactions;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Beneficiary> beneficiaries = new ArrayList<>();

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private EmploymentDetails employmentDetails;

    // Helper method to add beneficiary (Best Practice)
    public void addBeneficiary(Beneficiary beneficiary) {
        beneficiaries.add(beneficiary);
        beneficiary.setMember(this);
    }

    public void setEmploymentDetails(EmploymentDetails details) {
        this.employmentDetails = details;
        if (details != null) {
            details.setMember(this);
        }
    }

    // ---------------------------------------------------------

    private LocalDateTime membershipDate;

    private Boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        membershipDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MemberStatus {
        ACTIVE, SUSPENDED, EXITED, DECEASED
    }

    public enum RegistrationStatus {
        PENDING, PAID
    }
}