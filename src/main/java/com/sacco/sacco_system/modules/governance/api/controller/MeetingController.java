package com.sacco.sacco_system.modules.governance.api.controller;

import com.sacco.sacco_system.modules.core.dto.ApiResponse;
import com.sacco.sacco_system.modules.governance.domain.entity.Meeting;
import com.sacco.sacco_system.modules.governance.domain.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for managing committee meetings
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    /**
     * Get loans awaiting committee meeting
     */
    @GetMapping("/loans/awaiting")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLoansAwaitingMeeting() {
        List<Map<String, Object>> loans = meetingService.getLoansAwaitingMeeting();
        return ResponseEntity.ok(new ApiResponse<>(true, "Loans awaiting meeting retrieved", loans));
    }

    /**
     * Create a new meeting
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createMeeting(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        String title = (String) payload.get("title");
        String typeStr = (String) payload.get("meetingType");
        LocalDate date = LocalDate.parse((String) payload.get("meetingDate"));
        LocalTime time = LocalTime.parse((String) payload.get("meetingTime"));
        String venue = (String) payload.get("venue");

        @SuppressWarnings("unchecked")
        List<String> loanIdStrings = (List<String>) payload.get("loanIds");
        List<UUID> loanIds = loanIdStrings != null ?
                loanIdStrings.stream().map(UUID::fromString).toList() : null;

        Meeting.MeetingType type = Meeting.MeetingType.valueOf(typeStr);

        Meeting meeting = meetingService.createMeeting(title, type, date, time, venue, loanIds, userDetails.getUsername());

        return ResponseEntity.ok(new ApiResponse<>(true, "Meeting created successfully", Map.of(
                "meetingId", meeting.getId(),
                "meetingNumber", meeting.getMeetingNumber()
        )));
    }

    /**
     * Get all scheduled meetings
     */
    @GetMapping("/scheduled")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getScheduledMeetings() {
        List<Map<String, Object>> meetings = meetingService.getScheduledMeetings();
        return ResponseEntity.ok(new ApiResponse<>(true, "Scheduled meetings retrieved", meetings));
    }

    /**
     * Get all meetings (for secretary/chairperson dashboard)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllMeetings() {
        List<Map<String, Object>> meetings = meetingService.getAllMeetings();
        return ResponseEntity.ok(new ApiResponse<>(true, "All meetings retrieved", meetings));
    }

    /**
     * Get meeting details with agenda
     */
    @GetMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMeetingDetails(@PathVariable UUID meetingId) {
        Map<String, Object> meeting = meetingService.getMeetingWithAgenda(meetingId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Meeting details retrieved", meeting));
    }

    /**
     * Add loans to meeting agenda
     */
    @PostMapping("/{meetingId}/add-loans")
    public ResponseEntity<ApiResponse<Object>> addLoansToAgenda(
            @PathVariable UUID meetingId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        @SuppressWarnings("unchecked")
        List<String> loanIdStrings = (List<String>) payload.get("loanIds");
        List<UUID> loanIds = loanIdStrings.stream().map(UUID::fromString).toList();

        meetingService.addLoansToAgenda(meetingId, loanIds, userDetails.getUsername());

        return ResponseEntity.ok(new ApiResponse<>(true, "Loans added to meeting agenda"));
    }

    /**
     * Update meeting details (date, time, venue) - Only for SCHEDULED meetings
     */
    @PatchMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<Object>> updateMeeting(
            @PathVariable UUID meetingId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        String title = (String) payload.get("title");
        LocalDate date = payload.containsKey("meetingDate") ?
                LocalDate.parse((String) payload.get("meetingDate")) : null;
        LocalTime time = payload.containsKey("meetingTime") ?
                LocalTime.parse((String) payload.get("meetingTime")) : null;
        String venue = (String) payload.get("venue");

        meetingService.updateMeeting(meetingId, title, date, time, venue, userDetails.getUsername());

        return ResponseEntity.ok(new ApiResponse<>(true, "Meeting updated successfully"));
    }

    /**
     * Update meeting status
     */
    @PatchMapping("/{meetingId}/status")
    public ResponseEntity<ApiResponse<Object>> updateMeetingStatus(
            @PathVariable UUID meetingId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        Meeting.MeetingStatus status = Meeting.MeetingStatus.valueOf(payload.get("status"));
        meetingService.updateMeetingStatus(meetingId, status, userDetails.getUsername());

        return ResponseEntity.ok(new ApiResponse<>(true, "Meeting status updated"));
    }
}

