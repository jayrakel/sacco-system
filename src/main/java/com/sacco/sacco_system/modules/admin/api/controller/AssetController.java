package com.sacco.sacco_system.modules.admin.api.controller;

import com.sacco.sacco_system.modules.admin.domain.entity.Asset;
import com.sacco.sacco_system.modules.admin.domain.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Asset Controller
 * Manages fixed assets - registration, depreciation, disposal
 */
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssetController {

    private final AssetService assetService;

    /**
     * Register a new asset
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerAsset(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String category = (String) request.get("category");
            String serialNumber = (String) request.get("serialNumber");
            BigDecimal purchaseCost = new BigDecimal(request.get("purchaseCost").toString());
            LocalDate purchaseDate = LocalDate.parse((String) request.get("purchaseDate"));
            Integer usefulLifeYears = Integer.parseInt(request.get("usefulLifeYears").toString());
            BigDecimal salvageValue = request.get("salvageValue") != null ?
                    new BigDecimal(request.get("salvageValue").toString()) : BigDecimal.ZERO;

            Asset asset = assetService.registerAsset(name, category, serialNumber,
                    purchaseCost, purchaseDate, usefulLifeYears, salvageValue);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Asset registered successfully",
                    "data", asset
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Calculate depreciation for a specific asset
     */
    @PostMapping("/{assetId}/depreciate")
    public ResponseEntity<Map<String, Object>> calculateDepreciation(@PathVariable UUID assetId) {
        try {
            Asset asset = assetService.calculateDepreciation(assetId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Depreciation calculated",
                    "data", asset
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Calculate depreciation for all active assets
     */
    @PostMapping("/depreciate-all")
    public ResponseEntity<Map<String, Object>> calculateAllDepreciation() {
        try {
            List<Asset> assets = assetService.calculateAllDepreciation();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Depreciation calculated for " + assets.size() + " assets",
                    "data", assets
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Dispose of an asset
     */
    @PostMapping("/{assetId}/dispose")
    public ResponseEntity<Map<String, Object>> disposeAsset(
            @PathVariable UUID assetId,
            @RequestBody Map<String, Object> request) {
        try {
            LocalDate disposalDate = LocalDate.parse((String) request.get("disposalDate"));
            BigDecimal disposalValue = new BigDecimal(request.get("disposalValue").toString());
            String notes = (String) request.get("notes");

            Asset asset = assetService.disposeAsset(assetId, disposalDate, disposalValue, notes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Asset disposed successfully",
                    "data", asset
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Mark asset as lost
     */
    @PostMapping("/{assetId}/lost")
    public ResponseEntity<Map<String, Object>> markAsLost(
            @PathVariable UUID assetId,
            @RequestBody Map<String, String> request) {
        try {
            String notes = request.get("notes");
            Asset asset = assetService.markAsLost(assetId, notes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Asset marked as lost",
                    "data", asset
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get all assets
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAssets() {
        List<Asset> assets = assetService.getAllAssets();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", assets
        ));
    }

    /**
     * Get active assets only
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveAssets() {
        List<Asset> assets = assetService.getActiveAssets();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", assets
        ));
    }

    /**
     * Get assets by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getAssetsByCategory(@PathVariable String category) {
        List<Asset> assets = assetService.getAssetsByCategory(category);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", assets
        ));
    }

    /**
     * Get asset statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = assetService.getAssetStatistics();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
        ));
    }

    /**
     * Get single asset
     */
    @GetMapping("/{assetId}")
    public ResponseEntity<Map<String, Object>> getAsset(@PathVariable UUID assetId) {
        try {
            Asset asset = assetService.getAsset(assetId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", asset
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}

