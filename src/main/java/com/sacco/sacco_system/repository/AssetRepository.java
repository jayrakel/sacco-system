package com.sacco.sacco_system.repository;
import com.sacco.sacco_system.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AssetRepository extends JpaRepository<Asset, UUID> {}