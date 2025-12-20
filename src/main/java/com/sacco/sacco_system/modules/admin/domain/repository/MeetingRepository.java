package com.sacco.sacco_system.modules.admin.domain.repository;

import com.sacco.sacco_system.modules.admin.domain.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    List<Meeting> findByStatus(Meeting.MeetingStatus status);

    List<Meeting> findByMeetingDateBetween(LocalDate startDate, LocalDate endDate);

    Optional<Meeting> findByMeetingNumber(String meetingNumber);

    List<Meeting> findByStatusOrderByMeetingDateDesc(Meeting.MeetingStatus status);

    // Find upcoming meetings
    List<Meeting> findByMeetingDateAfterAndStatusInOrderByMeetingDateAsc(
            LocalDate date,
            List<Meeting.MeetingStatus> statuses);
}

