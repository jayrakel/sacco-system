package com.sacco.sacco_system.modules.deposit.domain.service;

import com.sacco.sacco_system.modules.deposit.api.dto.DepositProductDTO;
import com.sacco.sacco_system.modules.deposit.domain.entity.DepositProduct;
import com.sacco.sacco_system.modules.deposit.domain.entity.DepositProductStatus;
import com.sacco.sacco_system.modules.deposit.domain.repository.DepositProductRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Deposit Product Service
 * Manages custom contribution products
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepositProductService {

    private final DepositProductRepository depositProductRepository;

    /**
     * Create a new deposit product
     */
    @Transactional
    public DepositProductDTO createProduct(Member createdBy, DepositProductDTO dto) {
        // Validate unique name
        if (depositProductRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("A product with this name already exists");
        }

        DepositProduct product = DepositProduct.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .targetAmount(dto.getTargetAmount())
                .status(DepositProductStatus.ACTIVE)
                .createdBy(createdBy)
                .build();

        DepositProduct saved = depositProductRepository.save(product);
        log.info("Created deposit product: {} by {}", saved.getName(), 
                createdBy != null ? createdBy.getEmail() : "SYSTEM/ADMIN");
        
        return convertToDTO(saved);
    }

    /**
     * Update deposit product
     */
    @Transactional
    public DepositProductDTO updateProduct(UUID id, DepositProductDTO dto) {
        DepositProduct product = depositProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if name changed and is unique
        if (!product.getName().equals(dto.getName()) && 
            depositProductRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("A product with this name already exists");
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setTargetAmount(dto.getTargetAmount());
        product.setStatus(dto.getStatus());

        DepositProduct saved = depositProductRepository.save(product);
        return convertToDTO(saved);
    }

    /**
     * Get all active products
     */
    public List<DepositProductDTO> getActiveProducts() {
        return depositProductRepository.findByStatus(DepositProductStatus.ACTIVE).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all products (for admin)
     */
    public List<DepositProductDTO> getAllProducts() {
        return depositProductRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get product by ID
     */
    public DepositProductDTO getProductById(UUID id) {
        DepositProduct product = depositProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return convertToDTO(product);
    }

    /**
     * Close product (stop accepting contributions)
     */
    @Transactional
    public DepositProductDTO closeProduct(UUID id) {
        DepositProduct product = depositProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setStatus(DepositProductStatus.CLOSED);
        DepositProduct saved = depositProductRepository.save(product);
        
        log.info("Closed deposit product: {}", saved.getName());
        return convertToDTO(saved);
    }

    /**
     * Delete product (only if no contributions)
     */
    @Transactional
    public void deleteProduct(UUID id) {
        DepositProduct product = depositProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getCurrentAmount().signum() > 0) {
            throw new IllegalStateException("Cannot delete product with existing contributions");
        }

        depositProductRepository.delete(product);
        log.info("Deleted deposit product: {}", product.getName());
    }

    /**
     * Convert entity to DTO
     */
    private DepositProductDTO convertToDTO(DepositProduct product) {
        return DepositProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .targetAmount(product.getTargetAmount())
                .currentAmount(product.getCurrentAmount())
                .status(product.getStatus())
                .createdById(product.getCreatedBy() != null ? product.getCreatedBy().getId() : null)
                .createdByName(product.getCreatedBy() != null ? 
                        product.getCreatedBy().getFirstName() + " " + product.getCreatedBy().getLastName() : 
                        "System/Admin")
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
