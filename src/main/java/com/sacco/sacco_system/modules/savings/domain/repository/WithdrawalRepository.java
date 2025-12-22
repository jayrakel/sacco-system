package com.sacco.sacco_system.modules.savings.domain.repository;

import com.sacco.sacco_system.modules.savings.domain.entity.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, UUID> {
    // Find withdrawals by request date range (matches Withdrawal.requestDate)
    List<Withdrawal> findByRequestDateBetween(LocalDateTime start, LocalDateTime end);
}