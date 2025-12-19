package com.sacco.sacco_system.modules.loans.repository;

import com.sacco.sacco_system.modules.loans.model.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, UUID> {
}