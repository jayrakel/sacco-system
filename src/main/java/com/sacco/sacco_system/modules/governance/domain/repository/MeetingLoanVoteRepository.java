package com.sacco.sacco_system.modules.governance.domain.repository;

import com.sacco.sacco_system.modules.governance.domain.entity.MeetingLoanAgenda;
import com.sacco.sacco_system.modules.governance.domain.entity.MeetingLoanVote;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingLoanVoteRepository extends JpaRepository<MeetingLoanVote, UUID> {

    List<MeetingLoanVote> findByAgendaItem(MeetingLoanAgenda agendaItem);

    Optional<MeetingLoanVote> findByAgendaItemAndVoter(MeetingLoanAgenda agendaItem, Member voter);

    boolean existsByAgendaItemAndVoter(MeetingLoanAgenda agendaItem, Member voter);

    long countByAgendaItem(MeetingLoanAgenda agendaItem);

    long countByAgendaItemAndDecision(MeetingLoanAgenda agendaItem, MeetingLoanVote.VoteDecision decision);

    @Query("SELECT COUNT(v) FROM MeetingLoanVote v WHERE v.agendaItem.meeting.id = :meetingId")
    long countVotesByMeetingId(UUID meetingId);
}

