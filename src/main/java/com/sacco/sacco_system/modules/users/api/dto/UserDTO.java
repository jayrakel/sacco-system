package com.sacco.sacco_system.modules.users.api.dto;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;
    private String email;
    private String officialEmail;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private User.Role role;
    private boolean enabled;
    private boolean emailVerified;
    private boolean mustChangePassword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
