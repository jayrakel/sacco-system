package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GuarantorRepository extends JpaRepository<Guarantor, UUID> {
    List<Guarantor> findByLoan(Loan loan);
    long countByLoanAndStatus(Loan loan, Guarantor.GuarantorStatus status);

    // Find all guarantor requests for a member (all statuses)
    List<Guarantor> findByMemberId(UUID memberId);

    // Find guarantor requests for a member by status
    List<Guarantor> findByMemberIdAndStatus(UUID memberId, Guarantor.GuarantorStatus status);

    /**
     * ✅ Critical for Member Exit Validation.
     * Checks if a member is an ACCEPTED guarantor for any loans that are currently Active/Disbursed.
     */
    @Query("SELECT COUNT(g) > 0 FROM Guarantor g WHERE g.member.id = :memberId AND g.status = 'ACCEPTED' AND g.loan.status IN :statuses")
    boolean existsByMemberIdAndLoanStatusIn(@Param("memberId") UUID memberId, @Param("statuses") List<Loan.LoanStatus> statuses);
}