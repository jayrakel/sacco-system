package com.sacco.sacco_system.modules.savings.domain.repository;

import com.sacco.sacco_system.modules.savings.domain.entity.SavingsProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsProductRepository;

@Repository
public interface SavingsProductRepository extends JpaRepository<SavingsProduct, UUID> {
}



