package com.sacco.sacco_system.modules.admin.domain.service.systemsetting;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.impl.SystemSettingFileManager;
import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.impl.SystemSettingReader;
import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.impl.SystemSettingWriter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    // ✅ Injecting the sub-files
    private final SystemSettingReader reader;
    private final SystemSettingWriter writer;
    private final SystemSettingFileManager fileManager;

    // --- READ OPERATIONS ---

    public List<SystemSetting> getAllSettings() {
        return reader.getAllSettings();
    }

    public Optional<String> getSetting(String key) {
        return reader.getSetting(key);
    }

    public String getString(String key, String defaultValue) {
        return reader.getString(key, defaultValue);
    }

    public double getDouble(String key) {
        return reader.getDouble(key);
    }

    public double getDouble(String key, double defaultValue) {
        return reader.getDouble(key, defaultValue);
    }

    public BigDecimal getBigDecimal(String key, String defaultValue) {
        return reader.getBigDecimal(key, defaultValue);
    }

    public Integer getInteger(String key, String defaultValue) {
        return reader.getInteger(key, defaultValue);
    }

    // --- WRITE OPERATIONS ---

    public SystemSetting createOrUpdate(String key, String value, String description) {
        return writer.createOrUpdate(key, value, description);
    }

    public SystemSetting updateSetting(String key, String value) {
        return writer.updateSetting(key, value);
    }

    // --- FILE OPERATIONS ---

    public SystemSetting uploadSettingImage(String key, MultipartFile file) throws IOException {
        return fileManager.uploadSettingImage(key, file);
    }
}