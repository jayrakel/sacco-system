package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    
    private String address;
    
    private LocalDate dateOfBirth;
    
    @Enumerated(EnumType.STRING)
    private MemberStatus status = MemberStatus.ACTIVE;
    
    private BigDecimal totalShares = BigDecimal.ZERO;
    
    private BigDecimal totalSavings = BigDecimal.ZERO;
    
    private LocalDateTime joinDate;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        joinDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum MemberStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}
