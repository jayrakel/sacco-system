package com.sacco.sacco_system.modules.governance.domain.repository;

import com.sacco.sacco_system.modules.governance.domain.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    Optional<Meeting> findByMeetingNumber(String meetingNumber);

    List<Meeting> findByStatusOrderByMeetingDateDesc(Meeting.MeetingStatus status);

    List<Meeting> findByMeetingDateBetweenOrderByMeetingDateDesc(LocalDate startDate, LocalDate endDate);

    List<Meeting> findByMeetingTypeAndStatusOrderByMeetingDateDesc(Meeting.MeetingType type, Meeting.MeetingStatus status);
}

