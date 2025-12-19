package com.sacco.sacco_system.modules.members.repository;

import com.sacco.sacco_system.modules.members.model.ShareCapital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShareCapitalRepository extends JpaRepository<ShareCapital, UUID> {
    Optional<ShareCapital> findByMemberId(Long memberId);
    
    @Query("SELECT SUM(sc.paidAmount) FROM ShareCapital sc")
    BigDecimal getTotalShareCapital();
    
    @Query("SELECT COUNT(sc) FROM ShareCapital sc WHERE sc.paidAmount > 0")
    long countShareholdersWithPaidShares();
}
