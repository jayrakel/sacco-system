package com.sacco.sacco_system.modules.users.domain.service;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import com.sacco.sacco_system.modules.users.api.dto.UserDTO;
import com.sacco.sacco_system.modules.users.api.dto.CreateUserRequest;
import com.sacco.sacco_system.modules.users.api.dto.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional; // ✅ Added Import
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(String firstName, String lastName, String email, String phoneNumber, User.Role role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("A user with this email address already exists.");
        }

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);

        User newUser = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phoneNumber)
                .role(role)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .mustChangePassword(true)
                .emailVerified(false)
                .active(true)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Created user account for: {}", email);

        return savedUser;
    }

    /**
     * Create bootstrap user (for system initialization)
     */
    @Transactional
    public User createBootstrapUser(String firstName, String lastName, String email, String officialEmail,
                                    String phoneNumber, String rawPassword, User.Role role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("A user with this email address already exists.");
        }

        User newUser = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .officialEmail(officialEmail)
                .phoneNumber(phoneNumber)
                .role(role)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .mustChangePassword(true)
                .emailVerified(true)
                .active(true)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Created bootstrap user account for: {} with role: {}", email, role);

        return savedUser;
    }

    @Transactional
    public UserDTO createUserFromRequest(CreateUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("A user with this email address already exists.");
        }

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);

        User newUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .officialEmail(request.getOfficialEmail())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .mustChangePassword(true)
                .emailVerified(false)
                .active(true)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Created user account for: {} with role: {}", savedUser.getEmail(), savedUser.getRole());

        return convertToDTO(savedUser);
    }

    @Transactional
    public void adminVerifyUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmailVerified(true);
        user.setActive(true);
        userRepository.save(user);
        log.info("Admin manually verified user: {}", user.getEmail());
    }

    @Transactional
    public void adminResetPassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
        log.info("Admin reset password for user: {}", user.getEmail());
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserDTO> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    // Existing method returning DTO (Keep this for API)
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    // ✅ NEW METHOD ADDED FOR LOAN MODULE INTEGRATION
    // This allows internal services (like LoanController) to get the raw User Entity
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public UserDTO updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getOfficialEmail() != null) user.setOfficialEmail(request.getOfficialEmail());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getEnabled() != null) user.setActive(request.getEnabled());
        if (request.getEmailVerified() != null) user.setEmailVerified(request.getEmailVerified());

        User updated = userRepository.save(user);
        log.info("Updated user: {}", updated.getEmail());

        return convertToDTO(updated);
    }

    @Transactional
    public void disableUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        user.setUserStatus(User.UserStatus.DISABLED);
        userRepository.save(user);
        log.info("Disabled user: {}", user.getEmail());
    }

    @Transactional
    public void enableUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        user.setUserStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("Enabled user: {}", user.getEmail());
    }

    public UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .officialEmail(user.getOfficialEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .mustChangePassword(user.isMustChangePassword())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}