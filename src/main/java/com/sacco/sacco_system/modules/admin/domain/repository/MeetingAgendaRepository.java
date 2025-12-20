package com.sacco.sacco_system.modules.admin.domain.repository;

import com.sacco.sacco_system.modules.admin.domain.entity.Meeting;
import com.sacco.sacco_system.modules.admin.domain.entity.MeetingAgenda;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingAgendaRepository extends JpaRepository<MeetingAgenda, UUID> {

    List<MeetingAgenda> findByMeetingOrderByAgendaNumberAsc(Meeting meeting);

    List<MeetingAgenda> findByMeetingAndStatus(Meeting meeting, MeetingAgenda.AgendaStatus status);

    Optional<MeetingAgenda> findByLoan(Loan loan);

    List<MeetingAgenda> findByStatusIn(List<MeetingAgenda.AgendaStatus> statuses);

    long countByMeetingAndStatus(Meeting meeting, MeetingAgenda.AgendaStatus status);
}

