package com.sacco.sacco_system.modules.users.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Business identifier (system-generated, immutable)
    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String officialEmail; // SACCO email for administrative access (e.g., chairperson@sacco.com)

    @Column(nullable = false)
    private String passwordHash;

    private String firstName;
    private String lastName;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus userStatus = UserStatus.ACTIVE;

    @Builder.Default
    private Boolean active = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Builder.Default
    private boolean mustChangePassword = false;

    private String createdBy;

    private String updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (userId == null) {
            userId = UUID.randomUUID();
        }
        if (username == null) {
            username = email; // Default username to email
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- UserDetails Implementation ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return userStatus != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return active && userStatus == UserStatus.ACTIVE;
    }

    public enum UserStatus {
        ACTIVE, LOCKED, DISABLED
    }

    public enum Role {
        MEMBER, ADMIN, LOAN_OFFICER, TELLER, CHAIRPERSON, SECRETARY, TREASURER, ASSISTANT, ASSISTANT_LOAN_OFFICER, ASSISTANT_CHAIRPERSON, ASSISTANT_SECRETARY
    }
}
