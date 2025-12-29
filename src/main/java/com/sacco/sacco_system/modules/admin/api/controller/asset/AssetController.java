package com.sacco.sacco_system.modules.admin.api.controller.asset;

import com.sacco.sacco_system.modules.admin.api.controller.asset.handler.AssetReadHandler;
import com.sacco.sacco_system.modules.admin.api.controller.asset.handler.AssetWriteHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // ✅ Ensures frontend can connect
public class AssetController {

    private final AssetReadHandler readHandler;
    private final AssetWriteHandler writeHandler;

    // --- READ OPERATIONS ---
    // ✅ FIX: Using 'hasAnyRole' handles the 'ROLE_' prefix automatically.
    // Also keeping 'hasAnyAuthority' just in case you store them without the prefix.

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getAllAssets() {
        return readHandler.getAllAssets();
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getActiveAssets() {
        return readHandler.getActiveAssets();
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getAssetsByCategory(@PathVariable String category) {
        return readHandler.getAssetsByCategory(category);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getStatistics() {
        return readHandler.getStatistics();
    }

    @GetMapping("/{assetId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getAsset(@PathVariable UUID assetId) {
        return readHandler.getAsset(assetId);
    }

    // --- WRITE OPERATIONS ---

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<?> registerAsset(@RequestBody Map<String, Object> request) {
        return writeHandler.registerAsset(request);
    }

    @PutMapping("/{assetId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<?> updateAsset(@PathVariable UUID assetId, @RequestBody Map<String, Object> request) {
        return writeHandler.updateAsset(assetId, request);
    }

    @DeleteMapping("/{assetId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteAsset(@PathVariable UUID assetId) {
        return writeHandler.deleteAsset(assetId);
    }

    @PostMapping("/{assetId}/depreciate")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<?> calculateDepreciation(@PathVariable UUID assetId) {
        return writeHandler.calculateDepreciation(assetId);
    }

    @PostMapping("/depreciate-all")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<?> calculateAllDepreciation() {
        return writeHandler.calculateAllDepreciation();
    }

    @PostMapping("/{assetId}/dispose")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<?> disposeAsset(
            @PathVariable UUID assetId,
            @RequestBody Map<String, Object> request) {
        return writeHandler.disposeAsset(assetId, request);
    }

    @PostMapping("/{assetId}/lost")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<?> markAsLost(
            @PathVariable UUID assetId,
            @RequestBody Map<String, String> request) {
        return writeHandler.markAsLost(assetId, request);
    }
}