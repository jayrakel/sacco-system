package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LoanProductRepository extends JpaRepository<LoanProduct, UUID> {}