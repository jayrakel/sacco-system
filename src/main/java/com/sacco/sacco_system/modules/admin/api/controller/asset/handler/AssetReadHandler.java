package com.sacco.sacco_system.modules.admin.api.controller.asset.handler;

import com.sacco.sacco_system.modules.admin.domain.service.asset.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssetReadHandler {

    private final AssetService service;

    public ResponseEntity<?> getAllAssets() {
        return ResponseEntity.ok(Map.of("success", true, "data", service.getAllAssets()));
    }

    public ResponseEntity<?> getActiveAssets() {
        return ResponseEntity.ok(Map.of("success", true, "data", service.getActiveAssets()));
    }

    public ResponseEntity<?> getAssetsByCategory(String category) {
        return ResponseEntity.ok(Map.of("success", true, "data", service.getAssetsByCategory(category)));
    }

    public ResponseEntity<?> getStatistics() {
        return ResponseEntity.ok(Map.of("success", true, "data", service.getAssetStatistics()));
    }

    public ResponseEntity<?> getAsset(UUID assetId) {
        return ResponseEntity.ok(Map.of("success", true, "data", service.getAsset(assetId)));
    }
}