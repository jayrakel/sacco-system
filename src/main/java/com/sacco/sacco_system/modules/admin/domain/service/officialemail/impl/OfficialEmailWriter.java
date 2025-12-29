package com.sacco.sacco_system.modules.admin.domain.service.officialemail.impl;

import com.sacco.sacco_system.annotation.Loggable;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OfficialEmailWriter {

    private final UserRepository userRepository;

    @Transactional
    @Loggable(action = "ASSIGN_OFFICIAL_EMAIL", category = "ADMIN")
    public Map<String, Object> assignOfficialEmail(UUID userId, String officialEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!officialEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (userRepository.findByOfficialEmail(officialEmail).isPresent()) {
            throw new IllegalArgumentException("This official email is already assigned to another user");
        }

        if (user.getRole() == User.Role.MEMBER) {
            throw new IllegalArgumentException("Cannot assign official email to regular members");
        }

        user.setOfficialEmail(officialEmail);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("personalEmail", user.getEmail());
        response.put("officialEmail", user.getOfficialEmail());
        response.put("role", user.getRole());
        return response;
    }

    @Transactional
    @Loggable(action = "REMOVE_OFFICIAL_EMAIL", category = "ADMIN")
    public void removeOfficialEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setOfficialEmail(null);
        userRepository.save(user);
    }
}