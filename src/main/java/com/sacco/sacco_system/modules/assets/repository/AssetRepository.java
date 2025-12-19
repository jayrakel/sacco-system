package com.sacco.sacco_system.modules.assets.repository;
import com.sacco.sacco_system.modules.assets.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AssetRepository extends JpaRepository<Asset, UUID> {}