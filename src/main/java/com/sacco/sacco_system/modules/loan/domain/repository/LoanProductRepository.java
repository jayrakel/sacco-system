package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, UUID> {
}



