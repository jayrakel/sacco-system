package com.sacco.sacco_system.modules.admin.domain.service;

import com.sacco.sacco_system.modules.admin.domain.entity.Asset;
import com.sacco.sacco_system.modules.admin.domain.repository.AssetRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Asset Management Service
 * Handles fixed asset registration, depreciation, and disposal
 * Integrated with accounting system
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssetService {

    private final AssetRepository assetRepository;
    private final AccountingService accountingService;

    /**
     * Register a new asset
     */
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

        // ✅ POST TO ACCOUNTING
        accountingService.postAssetPurchase(saved, "Asset Purchase: " + name);
        // Creates: DEBIT Fixed Assets (1300), CREDIT Cash (1020)

        log.info("Asset registered: {} - KES {}", name, purchaseCost);

        return saved;
    }

    /**
     * Calculate and record depreciation for an asset
     * Uses straight-line depreciation method
     */
    public Asset calculateDepreciation(UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        if (asset.getStatus() != Asset.AssetStatus.ACTIVE) {
            throw new RuntimeException("Can only depreciate active assets");
        }

        if (asset.getUsefulLifeYears() == null || asset.getUsefulLifeYears() == 0) {
            throw new RuntimeException("Useful life not set for asset");
        }

        // Calculate annual depreciation (straight-line method)
        BigDecimal depreciableAmount = asset.getPurchaseCost().subtract(asset.getSalvageValue());
        BigDecimal annualDepreciation = depreciableAmount
                .divide(BigDecimal.valueOf(asset.getUsefulLifeYears()), 2, RoundingMode.HALF_UP);

        // Calculate depreciation to date
        long monthsOwned = ChronoUnit.MONTHS.between(asset.getPurchaseDate(), LocalDate.now());
        BigDecimal monthlyDepreciation = annualDepreciation.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal totalDepreciation = monthlyDepreciation.multiply(BigDecimal.valueOf(monthsOwned));

        // Don't exceed depreciable amount
        if (totalDepreciation.compareTo(depreciableAmount) > 0) {
            totalDepreciation = depreciableAmount;
        }

        // Update asset
        asset.setAccumulatedDepreciation(totalDepreciation);
        asset.setCurrentValue(asset.getPurchaseCost().subtract(totalDepreciation));

        Asset updated = assetRepository.save(asset);

        log.info("Depreciation calculated for {}: KES {} (Total: KES {})",
                asset.getName(), totalDepreciation, asset.getCurrentValue());

        return updated;
    }

    /**
     * Calculate depreciation for all active assets
     */
    public List<Asset> calculateAllDepreciation() {
        List<Asset> activeAssets = assetRepository.findByStatus(Asset.AssetStatus.ACTIVE);

        List<Asset> updated = new java.util.ArrayList<>();
        for (Asset asset : activeAssets) {
            try {
                updated.add(calculateDepreciation(asset.getId()));
            } catch (Exception e) {
                log.warn("Failed to calculate depreciation for asset {}: {}",
                        asset.getId(), e.getMessage());
            }
        }

        log.info("Calculated depreciation for {} assets", updated.size());
        return updated;
    }

    /**
     * Dispose of an asset (sell or discard)
     */
    public Asset disposeAsset(UUID assetId, LocalDate disposalDate,
                             BigDecimal disposalValue, String notes) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        if (asset.getStatus() != Asset.AssetStatus.ACTIVE) {
            throw new RuntimeException("Asset already disposed or lost");
        }

        // Calculate final depreciation
        calculateDepreciation(assetId);

        // Update asset
        asset.setStatus(Asset.AssetStatus.DISPOSED);
        asset.setDisposalDate(disposalDate);
        asset.setDisposalValue(disposalValue);
        asset.setDisposalNotes(notes);

        Asset updated = assetRepository.save(asset);

        // Calculate gain/loss on disposal
        BigDecimal gainLoss = disposalValue.subtract(asset.getCurrentValue());

        log.info("Asset disposed: {} - Disposal value: KES {}, Book value: KES {}, Gain/Loss: KES {}",
                asset.getName(), disposalValue, asset.getCurrentValue(), gainLoss);

        // TODO: Post disposal to accounting
        // If gain: DEBIT Cash, CREDIT Fixed Assets, CREDIT Gain on Disposal
        // If loss: DEBIT Cash, DEBIT Loss on Disposal, CREDIT Fixed Assets

        return updated;
    }

    /**
     * Mark asset as lost
     */
    public Asset markAsLost(UUID assetId, String notes) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        asset.setStatus(Asset.AssetStatus.LOST);
        asset.setDisposalDate(LocalDate.now());
        asset.setDisposalNotes("LOST: " + notes);

        Asset updated = assetRepository.save(asset);

        log.warn("Asset marked as lost: {} - KES {}", asset.getName(), asset.getCurrentValue());

        return updated;
    }

    /**
     * Get all assets
     */
    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    /**
     * Get active assets
     */
    public List<Asset> getActiveAssets() {
        return assetRepository.findByStatus(Asset.AssetStatus.ACTIVE);
    }

    /**
     * Get assets by category
     */
    public List<Asset> getAssetsByCategory(String category) {
        return assetRepository.findByCategory(category);
    }

    /**
     * Get asset statistics
     */
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

    /**
     * Get asset by ID
     */
    public Asset getAsset(UUID assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
    }
}

