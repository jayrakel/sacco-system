package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.finance.domain.repository.GlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private final LoanProductRepository repository;
    private final GlMappingRepository glMappingRepository;

    public List<LoanProduct> getAllActiveProducts() {
        // You might want to filter by isActive=true here, or just return all for admin
        return repository.findAll();
    }

    // ✅ UPDATED: Auto-configure accounting fields from GL mappings
    @Transactional
    public LoanProduct createProduct(LoanProduct product) {
        // ✅ ALWAYS auto-generate productCode (per Domain Dictionary Phase B)
        // productCode is a unique system identifier, NEVER accept from user input
        product.setProductCode(generateProductCode(product.getProductName()));

        // ✅ Auto-configure accounting fields from GL mappings
        if (product.getReceivableAccountCode() == null || product.getReceivableAccountCode().isEmpty()) {
            glMappingRepository.findByEventName("LOAN_DISBURSEMENT")
                .ifPresent(mapping -> product.setReceivableAccountCode(mapping.getDebitAccountCode()));
        }

        if (product.getIncomeAccountCode() == null || product.getIncomeAccountCode().isEmpty()) {
            glMappingRepository.findByEventName("LOAN_REPAYMENT_INTEREST")
                .ifPresent(mapping -> product.setIncomeAccountCode(mapping.getCreditAccountCode()));
        }

        // Set default currency if not provided
        if (product.getCurrencyCode() == null || product.getCurrencyCode().isEmpty()) {
            product.setCurrencyCode("KES");
        }

        // Ensure active is set
        if (product.getActive() == null) {
            product.setActive(true);
        }

        return repository.save(product);
    }

    private String generateProductCode(String productName) {
        // Generate code like "LOAN-EMERGENCY" from "Emergency Loan"
        String baseCode = "LOAN-" + productName
            .toUpperCase()
            .replaceAll("[^A-Z0-9\\s]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("LOAN-?", "");

        // Check if code exists and add suffix if needed
        String finalCode = baseCode;
        int counter = 1;
        while (repository.findByProductCode(finalCode).isPresent()) {
            finalCode = baseCode + "-" + counter++;
        }

        return finalCode;
    }
}