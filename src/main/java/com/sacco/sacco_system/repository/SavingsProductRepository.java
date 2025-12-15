package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.SavingsProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SavingsProductRepository extends JpaRepository<SavingsProduct, UUID> {
}