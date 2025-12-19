package com.sacco.sacco_system.modules.system.repository;

import com.sacco.sacco_system.modules.system.model.Charge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChargeRepository extends JpaRepository<Charge, UUID> {
    List<Charge> findByMemberId(UUID memberId);
    List<Charge> findByLoanId(UUID loanId);
}