package com.sacco.sacco_system.modules.admin.domain.service.officialemail;

// ✅ Import workers from the handler sub-package
import com.sacco.sacco_system.modules.admin.domain.service.officialemail.impl.OfficialEmailReader;
import com.sacco.sacco_system.modules.admin.domain.service.officialemail.impl.OfficialEmailWriter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfficialEmailService {

    private final OfficialEmailReader reader;
    private final OfficialEmailWriter writer;

    // --- DELEGATE TO WRITER ---

    public Map<String, Object> assignOfficialEmail(UUID userId, String officialEmail) {
        return writer.assignOfficialEmail(userId, officialEmail);
    }

    public void removeOfficialEmail(UUID userId) {
        writer.removeOfficialEmail(userId);
    }

    // --- DELEGATE TO READER ---

    public Map<String, Object> getUserEmailDetails(UUID userId) {
        return reader.getUserEmailDetails(userId);
    }

    public Map<String, Object> generateSuggestion(UUID userId) {
        return reader.generateSuggestion(userId);
    }
}