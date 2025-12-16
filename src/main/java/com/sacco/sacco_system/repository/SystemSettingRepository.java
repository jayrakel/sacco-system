package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
    // Since 'key' is the ID, findById works, but we keep findByKey for code readability compatibility
    Optional<SystemSetting> findByKey(String key);
}