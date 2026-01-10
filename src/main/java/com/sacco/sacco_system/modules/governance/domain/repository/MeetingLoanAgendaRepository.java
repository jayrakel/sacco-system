package com.sacco.sacco_system.modules.governance.domain.repository;

import com.sacco.sacco_system.modules.governance.domain.entity.Meeting;
import com.sacco.sacco_system.modules.governance.domain.entity.MeetingLoanAgenda;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingLoanAgendaRepository extends JpaRepository<MeetingLoanAgenda, UUID> {

    List<MeetingLoanAgenda> findByMeetingOrderByAgendaOrderAsc(Meeting meeting);

    Optional<MeetingLoanAgenda> findByMeetingAndLoan(Meeting meeting, Loan loan);

    List<MeetingLoanAgenda> findByLoan(Loan loan);

    boolean existsByLoanAndStatus(Loan loan, MeetingLoanAgenda.AgendaStatus status);
}

