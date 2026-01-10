package com.sacco.sacco_system.modules.governance.api.controller;

import com.sacco.sacco_system.modules.core.dto.ApiResponse;
import com.sacco.sacco_system.modules.governance.domain.entity.MeetingLoanVote;
import com.sacco.sacco_system.modules.governance.domain.service.VotingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for committee voting on loans
 */
@RestController
@RequestMapping("/api/voting")
@RequiredArgsConstructor
public class VotingController {

    private final VotingService votingService;

    /**
     * Chairperson opens voting for a meeting
     * Can only open if meeting date/time has passed
     */
    @PostMapping("/meetings/{meetingId}/open")
    public ResponseEntity<ApiResponse<Object>> openVoting(
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        votingService.openVoting(meetingId, userDetails.getUsername());

        return ResponseEntity.ok(new ApiResponse<>(true, "Voting opened successfully"));
    }

    /**
     * Get loans available for voting (for logged-in committee member)
     */
    @GetMapping("/loans/available")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLoansForVoting(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<Map<String, Object>> loans = votingService.getLoansForVoting(userDetails.getUsername());

        return ResponseEntity.ok(new ApiResponse<>(true, "Available loans retrieved", loans));
    }

    /**
     * Committee member casts vote on a loan
     */
    @PostMapping("/cast")
    public ResponseEntity<ApiResponse<Object>> castVote(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID agendaItemId = UUID.fromString((String) payload.get("agendaItemId"));
        String decisionStr = (String) payload.get("decision");
        String comments = (String) payload.get("comments");

        MeetingLoanVote.VoteDecision decision = MeetingLoanVote.VoteDecision.valueOf(decisionStr);

        votingService.castVote(agendaItemId, userDetails.getUsername(), decision, comments);

        return ResponseEntity.ok(new ApiResponse<>(true, "Vote cast successfully"));
    }

    /**
     * Get voting results for a meeting
     */
    @GetMapping("/meetings/{meetingId}/results")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVotingResults(@PathVariable UUID meetingId) {

        Map<String, Object> results = votingService.getVotingResults(meetingId);

        return ResponseEntity.ok(new ApiResponse<>(true, "Voting results retrieved", results));
    }

    /**
     * Chairperson closes voting - no more votes can be cast
     */
    @PostMapping("/meetings/{meetingId}/close")
    public ResponseEntity<ApiResponse<Object>> closeVoting(
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        votingService.closeVoting(meetingId, userDetails.getUsername());

        return ResponseEntity.ok(new ApiResponse<>(true, "Voting closed. No more votes can be cast. Awaiting secretary to finalize results."));
    }

    /**
     * Secretary finalizes voting results, generates minutes, and forwards loans for disbursement
     */
    @PostMapping("/meetings/{meetingId}/finalize")
    public ResponseEntity<ApiResponse<Object>> finalizeVotingResults(
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        votingService.finalizeVotingResults(meetingId, userDetails.getUsername());

        return ResponseEntity.ok(new ApiResponse<>(true, "Voting results finalized. Minutes generated. Approved loans forwarded for disbursement."));
    }
}

