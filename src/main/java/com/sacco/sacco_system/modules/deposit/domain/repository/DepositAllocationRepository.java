package com.sacco.sacco_system.modules.deposit.domain.repository;

import com.sacco.sacco_system.modules.deposit.domain.entity.DepositAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DepositAllocationRepository extends JpaRepository<DepositAllocation, UUID> {
}
