package com.sacco.sacco_system.modules.assets.controller;

import com.sacco.sacco_system.modules.assets.model.Asset;
import com.sacco.sacco_system.modules.assets.repository.AssetRepository;
import com.sacco.sacco_system.modules.assets.service.AssetService; // ✅ Import Service
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetRepository assetRepository;
    private final AssetService assetService; // ✅ Inject Service

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(Map.of("success", true, "data", assetRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Asset asset) {
        // ✅ Use Service instead of Repository
        Asset savedAsset = assetService.purchaseAsset(asset);
        return ResponseEntity.ok(Map.of("success", true, "data", savedAsset));
    }
}