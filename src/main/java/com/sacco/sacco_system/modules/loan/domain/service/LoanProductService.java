package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private final LoanProductRepository repository;

    public List<LoanProduct> getAllActiveProducts() {
        // You might want to filter by isActive=true here, or just return all for admin
        return repository.findAll();
    }

    // ✅ ADDED: Method to handle product creation
    @Transactional
    public LoanProduct createProduct(LoanProduct product) {
        // Optional: Add validation here (e.g., check if account codes exist)
        return repository.save(product);
    }
}