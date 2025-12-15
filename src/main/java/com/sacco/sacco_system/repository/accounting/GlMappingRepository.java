package com.sacco.sacco_system.repository.accounting;
import com.sacco.sacco_system.entity.accounting.GlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GlMappingRepository extends JpaRepository<GlMapping, String> {
    Optional<GlMapping> findByEventName(String eventName);
}