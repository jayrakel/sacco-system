package com.sacco.sacco_system.modules.savings.repository;

import com.sacco.sacco_system.modules.savings.model.SavingsProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SavingsProductRepository extends JpaRepository<SavingsProduct, UUID> {
}