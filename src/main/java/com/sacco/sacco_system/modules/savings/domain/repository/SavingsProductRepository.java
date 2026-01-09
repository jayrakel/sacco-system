package com.sacco.sacco_system.modules.savings.domain.repository;

import com.sacco.sacco_system.modules.savings.domain.entity.SavingsProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavingsProductRepository extends JpaRepository<SavingsProduct, UUID> {
    Optional<SavingsProduct> findByProductCode(String productCode);

    boolean existsByProductCode(String productCode);
}



