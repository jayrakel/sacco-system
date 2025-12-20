package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanDisbursementRepository extends JpaRepository<LoanDisbursement, UUID> {

    Optional<LoanDisbursement> findByLoan(Loan loan);

    List<LoanDisbursement> findByStatus(LoanDisbursement.DisbursementStatus status);

    List<LoanDisbursement> findByMethod(LoanDisbursement.DisbursementMethod method);

    Optional<LoanDisbursement> findByDisbursementNumber(String disbursementNumber);

    long countByStatus(LoanDisbursement.DisbursementStatus status);
}

