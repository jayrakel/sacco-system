package com.sacco.sacco_system.modules.admin.domain.repository;

import com.sacco.sacco_system.modules.admin.domain.entity.AgendaVote;
import com.sacco.sacco_system.modules.admin.domain.entity.MeetingAgenda;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgendaVoteRepository extends JpaRepository<AgendaVote, UUID> {

    List<AgendaVote> findByAgenda(MeetingAgenda agenda);

    Optional<AgendaVote> findByAgendaAndMember(MeetingAgenda agenda, Member member);

    long countByAgenda(MeetingAgenda agenda);

    long countByAgendaAndVote(MeetingAgenda agenda, AgendaVote.VoteChoice vote);

    boolean existsByAgendaAndMember(MeetingAgenda agenda, Member member);
}

