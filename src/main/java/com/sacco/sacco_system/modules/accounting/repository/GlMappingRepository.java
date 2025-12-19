package com.sacco.sacco_system.modules.accounting.repository;
import com.sacco.sacco_system.modules.accounting.model.GlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GlMappingRepository extends JpaRepository<GlMapping, String> {
    Optional<GlMapping> findByEventName(String eventName);
}