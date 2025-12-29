package com.sacco.sacco_system.modules.admin.domain.service.systemsetting.impl;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SystemSettingReader {

    private final SystemSettingRepository repository;

    public List<SystemSetting> getAllSettings() {
        return repository.findAll();
    }

    public Optional<String> getSetting(String key) {
        return repository.findByKey(key).map(SystemSetting::getValue);
    }

    public String getString(String key, String defaultValue) {
        return repository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        return repository.findByKey(key)
                .map(s -> {
                    try {
                        return Double.parseDouble(s.getValue());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public BigDecimal getBigDecimal(String key, String defaultValue) {
        return repository.findByKey(key)
                .map(s -> {
                    try { return new BigDecimal(s.getValue()); }
                    catch (Exception e) { return new BigDecimal(defaultValue); }
                })
                .orElse(new BigDecimal(defaultValue));
    }

    public Integer getInteger(String key, String defaultValue) {
        return repository.findByKey(key)
                .map(s -> {
                    try { return Integer.parseInt(s.getValue()); }
                    catch (Exception e) { return Integer.parseInt(defaultValue); }
                })
                .orElse(Integer.parseInt(defaultValue));
    }
}