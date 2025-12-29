package com.sacco.sacco_system.modules.admin.domain.service.asset.impl;

import com.sacco.sacco_system.annotation.Loggable;
import com.sacco.sacco_system.modules.admin.domain.entity.Asset;
import com.sacco.sacco_system.modules.admin.domain.repository.AssetRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetWriter {

    private final AssetRepository assetRepository;
    private final AccountingService accountingService;

    // --- CREATE ---

    @Transactional
    @Loggable(action = "REGISTER_ASSET", category = "ASSET_MANAGEMENT")
    public Asset registerAsset(String name, String category, String serialNumber,
                               BigDecimal purchaseCost, LocalDate purchaseDate,
                               Integer usefulLifeYears, BigDecimal salvageValue) {

        Asset asset = Asset.builder()
                .name(name)
                .category(category)
                .serialNumber(serialNumber)
                .purchaseCost(purchaseCost)
                .purchaseDate(purchaseDate)
                .usefulLifeYears(usefulLifeYears)
                .salvageValue(salvageValue != null ? salvageValue : BigDecimal.ZERO)
                .accumulatedDepreciation(BigDecimal.ZERO)
                .currentValue(purchaseCost)
                .status(Asset.AssetStatus.ACTIVE)
                .build();

        Asset saved = assetRepository.save(asset);

        // Accounting Integration
        accountingService.postAssetPurchase(saved, "Asset Purchase: " + name);

        return saved;
    }

    // --- UPDATE (General Edit) ---  ✅ NEW CRUD FEATURE
    @Transactional
    @Loggable(action = "UPDATE_ASSET_DETAILS", category = "ASSET_MANAGEMENT")
    public Asset updateAssetDetails(UUID assetId, String name, String category, String serialNumber) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        // Only allow updating non-financial fields to prevent accounting mismatches
        if (name != null) asset.setName(name);
        if (category != null) asset.setCategory(category);
        if (serialNumber != null) asset.setSerialNumber(serialNumber);

        return assetRepository.save(asset);
    }

    // --- DELETE (Hard Delete) --- ✅ NEW CRUD FEATURE
    @Transactional
    @Loggable(action = "DELETE_ASSET", category = "ASSET_MANAGEMENT")
    public void deleteAsset(UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        // Strict Check: Prevent deleting assets that have history
        if (asset.getAccumulatedDepreciation().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Cannot delete asset with existing depreciation history. Use Disposal instead.");
        }

        // TODO: Ideally, we should also reverse the initial accounting entry here
        // accountingService.reverseAssetPurchase(asset);

        assetRepository.delete(asset);
    }

    // --- BUSINESS LOGIC (Depreciation/Disposal) ---

    @Transactional
    @Loggable(action = "CALCULATE_DEPRECIATION", category = "ASSET_MANAGEMENT")
    public Asset calculateDepreciation(UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        if (asset.getStatus() != Asset.AssetStatus.ACTIVE) {
            throw new RuntimeException("Can only depreciate active assets");
        }

        BigDecimal depreciableAmount = asset.getPurchaseCost().subtract(asset.getSalvageValue());
        BigDecimal annualDepreciation = depreciableAmount
                .divide(BigDecimal.valueOf(asset.getUsefulLifeYears()), 2, RoundingMode.HALF_UP);

        long monthsOwned = ChronoUnit.MONTHS.between(asset.getPurchaseDate(), LocalDate.now());
        BigDecimal monthlyDepreciation = annualDepreciation.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal totalDepreciation = monthlyDepreciation.multiply(BigDecimal.valueOf(monthsOwned));

        if (totalDepreciation.compareTo(depreciableAmount) > 0) {
            totalDepreciation = depreciableAmount;
        }

        asset.setAccumulatedDepreciation(totalDepreciation);
        asset.setCurrentValue(asset.getPurchaseCost().subtract(totalDepreciation));

        return assetRepository.save(asset);
    }

    @Transactional
    public List<Asset> calculateAllDepreciation() {
        List<Asset> activeAssets = assetRepository.findByStatus(Asset.AssetStatus.ACTIVE);
        List<Asset> updated = new ArrayList<>();

        for (Asset asset : activeAssets) {
            try {
                updated.add(calculateDepreciation(asset.getId()));
            } catch (Exception e) {
                log.warn("Depreciation calculation skipped for asset {}: {}", asset.getId(), e.getMessage());
            }
        }
        return updated;
    }

    @Transactional
    @Loggable(action = "DISPOSE_ASSET", category = "ASSET_MANAGEMENT")
    public Asset disposeAsset(UUID assetId, LocalDate disposalDate, BigDecimal disposalValue, String notes) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        calculateDepreciation(assetId);

        asset.setStatus(Asset.AssetStatus.DISPOSED);
        asset.setDisposalDate(disposalDate);
        asset.setDisposalValue(disposalValue);
        asset.setDisposalNotes(notes);

        return assetRepository.save(asset);
    }

    @Transactional
    @Loggable(action = "MARK_ASSET_LOST", category = "ASSET_MANAGEMENT")
    public Asset markAsLost(UUID assetId, String notes) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        asset.setStatus(Asset.AssetStatus.LOST);
        asset.setDisposalDate(LocalDate.now());
        asset.setDisposalNotes("LOST: " + notes);

        return assetRepository.save(asset);
    }
}