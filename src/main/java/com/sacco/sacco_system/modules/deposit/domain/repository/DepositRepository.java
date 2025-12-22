package com.sacco.sacco_system.modules.deposit.domain.repository;

import com.sacco.sacco_system.modules.deposit.domain.entity.Deposit;
import com.sacco.sacco_system.modules.deposit.domain.entity.DepositStatus;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, UUID> {
    
    List<Deposit> findByMemberOrderByCreatedAtDesc(Member member);
    
    List<Deposit> findByMemberAndStatus(Member member, DepositStatus status);
    
    Optional<Deposit> findByTransactionReference(String transactionReference);
    
    List<Deposit> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
