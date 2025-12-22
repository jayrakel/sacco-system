package com.sacco.sacco_system.modules.finance.domain.repository;

import com.sacco.sacco_system.modules.finance.domain.entity.Fine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface FineRepository extends JpaRepository<Fine, UUID> {

    List<Fine> findByMemberId(UUID memberId);

    List<Fine> findByMemberIdAndStatus(UUID memberId, Fine.FineStatus status);

    List<Fine> findByLoanId(UUID loanId);

    List<Fine> findByStatus(Fine.FineStatus status);

    @Query("SELECT SUM(f.amount) FROM Fine f WHERE f.member.id = :memberId AND f.status = 'PENDING'")
    BigDecimal getTotalPendingFinesByMember(@Param("memberId") UUID memberId);

    @Query("SELECT SUM(f.amount) FROM Fine f WHERE f.status = 'PAID'")
    BigDecimal getTotalFinesPaid();

    @Query("SELECT SUM(f.amount) FROM Fine f WHERE f.status = 'PENDING'")
    BigDecimal getTotalPendingFines();
}

