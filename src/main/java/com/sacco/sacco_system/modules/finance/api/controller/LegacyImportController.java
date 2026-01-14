package com.sacco.sacco_system.modules.finance.api.controller;

import com.sacco.sacco_system.modules.finance.domain.service.LegacyDataImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/legacy-import")
@RequiredArgsConstructor
public class LegacyImportController {

    private final LegacyDataImportService legacyDataImportService;

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedData(@RequestParam String folderPath) {
        // Run in a separate thread to avoid timeout
        new Thread(() -> legacyDataImportService.seedFromFolder(folderPath)).start();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Legacy data seeding initiated. Check server logs for progress.");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "date", required = false) String dateStr) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.toLowerCase().endsWith(".csv")
                    && !filename.toLowerCase().endsWith(".xlsx")
                    && !filename.toLowerCase().endsWith(".xls"))) {
                response.put("success", false);
                response.put("message", "Invalid file format. Only CSV, XLS, and XLSX are supported.");
                return ResponseEntity.badRequest().body(response);
            }

            LocalDate transactionDate = null;
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                try {
                    transactionDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("message", "Invalid date format. Use YYYY-MM-DD");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Run import in separate thread to avoid timeout
            LocalDate finalDate = transactionDate;
            new Thread(() -> {
                try {
                    legacyDataImportService.importFromUpload(
                        file.getInputStream(),
                        filename,
                        finalDate
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            response.put("success", true);
            response.put("message", "File upload initiated. Processing " + filename + ". Check server logs for progress.");
            response.put("filename", filename);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}