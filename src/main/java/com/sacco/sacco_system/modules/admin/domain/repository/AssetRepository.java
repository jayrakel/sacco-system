package com.sacco.sacco_system.modules.admin.domain.repository;
import com.sacco.sacco_system.modules.admin.domain.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import com.sacco.sacco_system.modules.admin.domain.repository.AssetRepository;
public interface AssetRepository extends JpaRepository<Asset, UUID> {}



