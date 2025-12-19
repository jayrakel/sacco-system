package com.sacco.sacco_system.modules.savings.domain.repository;

import com.sacco.sacco_system.modules.savings.domain.entity.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByWithdrawalDateBetween(LocalDate startDate, LocalDate endDate);
}
