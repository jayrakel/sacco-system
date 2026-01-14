package com.sacco.sacco_system.modules.finance.api.controller;

import com.sacco.sacco_system.modules.finance.domain.service.LegacyDataImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/import")
@RequiredArgsConstructor
public class LegacyImportController {

    private final LegacyDataImportService importService;

    // ✅ Updated to accept multiple files
    @PostMapping(value = "/legacy-history", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importLegacyHistory(@RequestParam("files") List<MultipartFile> files) {
        String result = importService.importHistory(files);
        return ResponseEntity.ok(result);
    }
}