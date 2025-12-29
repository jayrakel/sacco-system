package com.sacco.sacco_system.modules.admin.domain.service.systemsetting.impl;

import com.sacco.sacco_system.annotation.Loggable; // ✅ Import the annotation
import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemSettingWriter {

    private final SystemSettingRepository repository;

    @Transactional
    @Loggable(action = "UPDATE_SETTING", category = "ADMIN") // ✅ Auto-Audit enabled
    public SystemSetting updateSetting(String key, String value) {
        SystemSetting setting = repository.findByKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));

        setting.setValue(value);
        return repository.save(setting);
    }

    @Transactional
    @Loggable(action = "CREATE_OR_UPDATE_SETTING", category = "ADMIN") // ✅ Auto-Audit enabled
    public SystemSetting createOrUpdate(String key, String value, String description) {
        return repository.findByKey(key)
                .map(existing -> {
                    existing.setValue(value);
                    if (description != null && !description.isEmpty()) {
                        existing.setDescription(description);
                    }
                    return repository.save(existing);
                })
                .orElseGet(() -> {
                    String type = "STRING";
                    try { Double.parseDouble(value); type = "NUMBER"; } catch (Exception ignored) {}

                    return repository.save(SystemSetting.builder()
                            .key(key)
                            .value(value)
                            .description(description)
                            .dataType(type)
                            .build());
                });
    }
}