package com.sacco.sacco_system.modules.admin.domain.service.asset;

import com.sacco.sacco_system.modules.admin.domain.entity.Asset;
import com.sacco.sacco_system.modules.admin.domain.service.asset.impl.AssetReader;
import com.sacco.sacco_system.modules.admin.domain.service.asset.impl.AssetWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetReader reader;
    private final AssetWriter writer;

    // --- WRITE OPERATIONS ---

    public Asset registerAsset(String name, String category, String serialNumber,
                               BigDecimal purchaseCost, LocalDate purchaseDate,
                               Integer usefulLifeYears, BigDecimal salvageValue) {
        return writer.registerAsset(name, category, serialNumber, purchaseCost, purchaseDate, usefulLifeYears, salvageValue);
    }

    // ✅ New CRUD
    public Asset updateAssetDetails(UUID assetId, String name, String category, String serialNumber) {
        return writer.updateAssetDetails(assetId, name, category, serialNumber);
    }

    // ✅ New CRUD
    public void deleteAsset(UUID assetId) {
        writer.deleteAsset(assetId);
    }

    public Asset calculateDepreciation(UUID assetId) {
        return writer.calculateDepreciation(assetId);
    }

    public List<Asset> calculateAllDepreciation() {
        return writer.calculateAllDepreciation();
    }

    public Asset disposeAsset(UUID assetId, LocalDate disposalDate, BigDecimal disposalValue, String notes) {
        return writer.disposeAsset(assetId, disposalDate, disposalValue, notes);
    }

    public Asset markAsLost(UUID assetId, String notes) {
        return writer.markAsLost(assetId, notes);
    }

    // --- READ OPERATIONS ---

    public List<Asset> getAllAssets() {
        return reader.getAllAssets();
    }

    public List<Asset> getActiveAssets() {
        return reader.getActiveAssets();
    }

    public List<Asset> getAssetsByCategory(String category) {
        return reader.getAssetsByCategory(category);
    }

    public Map<String, Object> getAssetStatistics() {
        return reader.getAssetStatistics();
    }

    public Asset getAsset(UUID assetId) {
        return reader.getAsset(assetId);
    }
}