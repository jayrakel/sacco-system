package com.sacco.sacco_system.modules.member.domain.entity;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.service.MemberService;

import com.sacco.sacco_system.modules.auth.model.User;
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

    @NotBlank(message = "ID number is required")
    @Column(unique = true, nullable = false)
    private String idNumber;

    @Column(unique = true)
    private String kraPin;

    private String address;

    private LocalDate dateOfBirth;

    private String nextOfKinName;
    private String nextOfKinPhone;
    private String nextOfKinRelation;

    @Enumerated(EnumType.STRING)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus registrationStatus = RegistrationStatus.PENDING;

    private BigDecimal totalShares = BigDecimal.ZERO;

    private BigDecimal totalSavings = BigDecimal.ZERO;

    // âœ… ADDED: Link to User Login Account (Required by MemberService)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    // âœ… Relationships

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

    // ---------------------------------------------------------

    // âœ… RENAMED: joinDate -> registrationDate (Matches MemberService)
    private LocalDateTime registrationDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MemberStatus {
        ACTIVE, INACTIVE, SUSPENDED, DECEASED
    }

    public enum RegistrationStatus {
        PENDING, PAID
    }
}



