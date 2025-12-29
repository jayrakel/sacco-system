package com.sacco.sacco_system.modules.admin.api.controller.asset.handler;

import com.sacco.sacco_system.modules.admin.domain.entity.Asset;
import com.sacco.sacco_system.modules.admin.domain.service.asset.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssetWriteHandler {

    private final AssetService service;

    public ResponseEntity<?> registerAsset(Map<String, Object> request) {
        String name = (String) request.get("name");
        String category = (String) request.get("category");
        String serialNumber = (String) request.get("serialNumber");
        BigDecimal purchaseCost = new BigDecimal(request.get("purchaseCost").toString());
        LocalDate purchaseDate = LocalDate.parse((String) request.get("purchaseDate"));
        Integer usefulLifeYears = Integer.parseInt(request.get("usefulLifeYears").toString());
        BigDecimal salvageValue = request.get("salvageValue") != null ?
                new BigDecimal(request.get("salvageValue").toString()) : BigDecimal.ZERO;

        Asset asset = service.registerAsset(name, category, serialNumber,
                purchaseCost, purchaseDate, usefulLifeYears, salvageValue);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Asset registered successfully",
                "data", asset
        ));
    }

    public ResponseEntity<?> calculateDepreciation(UUID assetId) {
        Asset asset = service.calculateDepreciation(assetId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Depreciation calculated",
                "data", asset
        ));
    }

    public ResponseEntity<?> calculateAllDepreciation() {
        List<Asset> assets = service.calculateAllDepreciation();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Depreciation calculated for " + assets.size() + " assets",
                "data", assets
        ));
    }

    public ResponseEntity<?> disposeAsset(UUID assetId, Map<String, Object> request) {
        LocalDate disposalDate = LocalDate.parse((String) request.get("disposalDate"));
        BigDecimal disposalValue = new BigDecimal(request.get("disposalValue").toString());
        String notes = (String) request.get("notes");

        Asset asset = service.disposeAsset(assetId, disposalDate, disposalValue, notes);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Asset disposed successfully",
                "data", asset
        ));
    }

    public ResponseEntity<?> markAsLost(UUID assetId, Map<String, String> request) {
        String notes = request.get("notes");
        Asset asset = service.markAsLost(assetId, notes);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Asset marked as lost",
                "data", asset
        ));
    }

    // ✅ Added Update Capability (Matches service refactor)
    public ResponseEntity<?> updateAsset(UUID id, Map<String, Object> body) {
        String name = (String) body.get("name");
        String category = (String) body.get("category");
        String serial = (String) body.get("serialNumber");

        Asset asset = service.updateAssetDetails(id, name, category, serial);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Asset updated successfully",
                "data", asset
        ));
    }

    // ✅ Added Delete Capability (Matches service refactor)
    public ResponseEntity<?> deleteAsset(UUID id) {
        service.deleteAsset(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Asset deleted successfully"
        ));
    }
}