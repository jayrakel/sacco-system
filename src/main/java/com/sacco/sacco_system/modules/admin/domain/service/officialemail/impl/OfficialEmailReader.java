package com.sacco.sacco_system.modules.admin.domain.service.officialemail.impl;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OfficialEmailReader {

    private final UserRepository userRepository;

    public Map<String, Object> getUserEmailDetails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("personalEmail", user.getEmail());
        response.put("officialEmail", user.getOfficialEmail());
        response.put("role", user.getRole());
        response.put("hasOfficialEmail", user.getOfficialEmail() != null);
        return response;
    }

    public Map<String, Object> generateSuggestion(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fallback to "sacco.com" if env var not set
        String domain = System.getenv("SACCO_EMAIL_DOMAIN");
        if (domain == null || domain.isEmpty()) {
            domain = "sacco.com";
        }

        String cleanLastName = user.getLastName() != null ?
                user.getLastName().toLowerCase().replaceAll("[^a-z]", "") : "user";

        String suggested = String.format("%s.%s@%s",
                user.getRole().name().toLowerCase(),
                cleanLastName,
                domain
        );

        return Map.of(
                "suggestedEmail", suggested,
                "role", user.getRole()
        );
    }
}