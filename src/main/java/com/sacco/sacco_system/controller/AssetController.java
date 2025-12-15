package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.entity.Asset;
import com.sacco.sacco_system.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {
    private final AssetRepository assetRepository;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(Map.of("success", true, "data", assetRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Asset asset) {
        return ResponseEntity.ok(Map.of("success", true, "data", assetRepository.save(asset)));
    }
}