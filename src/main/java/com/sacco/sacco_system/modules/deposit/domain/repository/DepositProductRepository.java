package com.sacco.sacco_system.modules.deposit.domain.repository;

import com.sacco.sacco_system.modules.deposit.domain.entity.DepositProduct;
import com.sacco.sacco_system.modules.deposit.domain.entity.DepositProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepositProductRepository extends JpaRepository<DepositProduct, UUID> {
    
    List<DepositProduct> findByStatus(DepositProductStatus status);
    
    boolean existsByName(String name);
}
