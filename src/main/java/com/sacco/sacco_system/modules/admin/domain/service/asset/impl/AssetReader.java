package com.sacco.sacco_system.modules.admin.domain.service.asset.impl;

import com.sacco.sacco_system.modules.admin.domain.entity.Asset;
import com.sacco.sacco_system.modules.admin.domain.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetReader {

    private final AssetRepository assetRepository;

    public Asset getAsset(UUID assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
    }

    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    public List<Asset> getActiveAssets() {
        return assetRepository.findByStatus(Asset.AssetStatus.ACTIVE);
    }

    public List<Asset> getAssetsByCategory(String category) {
        return assetRepository.findByCategory(category);
    }

    public Map<String, Object> getAssetStatistics() {
        List<Asset> allAssets = assetRepository.findAll();

        long active = allAssets.stream()
                .filter(a -> a.getStatus() == Asset.AssetStatus.ACTIVE)
                .count();

        long disposed = allAssets.stream()
                .filter(a -> a.getStatus() == Asset.AssetStatus.DISPOSED)
                .count();

        long lost = allAssets.stream()
                .filter(a -> a.getStatus() == Asset.AssetStatus.LOST)
                .count();

        BigDecimal totalValue = assetRepository.getTotalAssetValue();
        BigDecimal totalDepreciation = assetRepository.getTotalDepreciation();

        return Map.of(
                "totalAssets", allAssets.size(),
                "activeCount", active,
                "disposedCount", disposed,
                "lostCount", lost,
                "totalValue", totalValue != null ? totalValue : BigDecimal.ZERO,
                "totalDepreciation", totalDepreciation != null ? totalDepreciation : BigDecimal.ZERO
        );
    }
}