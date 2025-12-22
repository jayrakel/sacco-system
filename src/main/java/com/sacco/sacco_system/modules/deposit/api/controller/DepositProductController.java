package com.sacco.sacco_system.modules.deposit.api.controller;

import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.deposit.api.dto.DepositProductDTO;
import com.sacco.sacco_system.modules.deposit.domain.service.DepositProductService;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deposit Product Controller
 * Admin endpoints for managing contribution products
 */
@RestController
@RequestMapping("/api/admin/deposit-products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CHAIRPERSON', 'TREASURER')")
public class DepositProductController {

    private final DepositProductService depositProductService;
    private final MemberRepository memberRepository;

    /**
     * Create a new deposit product
     * POST /api/admin/deposit-products
     */
    @PostMapping
    public ResponseEntity<?> createProduct(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DepositProductDTO dto) {
        
        try {
            // Try to find member record, but allow null for admin users
            Member createdBy = memberRepository.findByEmail(user.getEmail())
                    .orElse(null);

            DepositProductDTO product = depositProductService.createProduct(createdBy, dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Deposit product created successfully");
            response.put("product", product);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to create product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Update deposit product
     * PUT /api/admin/deposit-products/{id}
     */
    @PutMapping("/{id}")

    public ResponseEntity<?> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody DepositProductDTO dto) {
        
        try {
            DepositProductDTO product = depositProductService.updateProduct(id, dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Deposit product updated successfully");
            response.put("product", product);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to update product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all deposit products
     * GET /api/admin/deposit-products
     */
    @GetMapping
    public ResponseEntity<?> getAllProducts() {
        try {
            List<DepositProductDTO> products = depositProductService.getAllProducts();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get product by ID
     * GET /api/admin/deposit-products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable UUID id) {
        try {
            DepositProductDTO product = depositProductService.getProductById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("product", product);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Close deposit product
     * POST /api/admin/deposit-products/{id}/close
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<?> closeProduct(@PathVariable UUID id) {
        try {
            DepositProductDTO product = depositProductService.closeProduct(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Deposit product closed successfully");
            response.put("product", product);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Delete deposit product
     * DELETE /api/admin/deposit-products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable UUID id) {
        try {
            depositProductService.deleteProduct(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Deposit product deleted successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
