package com.sacco.sacco_system.modules.admin.domain.repository;

import com.sacco.sacco_system.modules.admin.domain.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {

    List<Asset> findByStatus(Asset.AssetStatus status);

    List<Asset> findByCategory(String category);

    @Query("SELECT SUM(a.currentValue) FROM Asset a WHERE a.status = 'ACTIVE'")
    BigDecimal getTotalAssetValue();

    @Query("SELECT SUM(a.accumulatedDepreciation) FROM Asset a WHERE a.status = 'ACTIVE'")
    BigDecimal getTotalDepreciation();
}

