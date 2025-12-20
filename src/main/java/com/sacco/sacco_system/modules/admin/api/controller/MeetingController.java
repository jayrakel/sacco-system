package com.sacco.sacco_system.modules.admin.api.controller;

import com.sacco.sacco_system.modules.admin.domain.entity.AgendaVote;
import com.sacco.sacco_system.modules.admin.domain.entity.Meeting;
import com.sacco.sacco_system.modules.admin.domain.entity.MeetingAgenda;
import com.sacco.sacco_system.modules.admin.domain.service.MeetingService;
import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    /**
     * SECRETARY: Create a new meeting
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMeeting(@RequestBody Map<String, String> request) {
        try {
            Meeting meeting = Meeting.builder()
                    .title(request.get("title"))
                    .description(request.get("description"))
                    .meetingDate(LocalDate.parse(request.get("meetingDate")))
                    .meetingTime(LocalTime.parse(request.get("meetingTime")))
                    .venue(request.get("venue"))
                    .type(Meeting.MeetingType.valueOf(request.get("type")))
                    .build();

            UUID userId = getCurrentUserId();
            Meeting created = meetingService.createMeeting(meeting, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Meeting created successfully");
            response.put("meeting", convertMeetingToMap(created));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * SECRETARY: Table a loan as agenda for meeting
     */
    @PostMapping("/{meetingId}/table-loan")
    public ResponseEntity<Map<String, Object>> tableLoan(
            @PathVariable UUID meetingId,
            @RequestBody Map<String, Object> request) {
        try {
            UUID loanId = UUID.fromString(request.get("loanId").toString());
            Integer agendaNumber = (Integer) request.get("agendaNumber");
            String notes = (String) request.get("notes");

            MeetingAgenda agenda = meetingService.tableLoanAsAgenda(meetingId, loanId, agendaNumber, notes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Loan tabled successfully. Meeting notifications sent to all members.");
            response.put("agenda", convertAgendaToMap(agenda));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * CHAIRPERSON: Open meeting (start the meeting session)
     */
    @PostMapping("/{meetingId}/open")
    public ResponseEntity<Map<String, Object>> openMeeting(@PathVariable UUID meetingId) {
        try {
            Meeting meeting = meetingService.openMeeting(meetingId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Meeting opened successfully. Agendas are ready for voting.");
            response.put("meeting", convertMeetingToMap(meeting));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * CHAIRPERSON: Open voting for specific agenda
     */
    @PostMapping("/agendas/{agendaId}/open-voting")
    public ResponseEntity<Map<String, Object>> openVoting(@PathVariable UUID agendaId) {
        try {
            MeetingAgenda agenda = meetingService.openVoting(agendaId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voting opened. Members have been notified.");
            response.put("agenda", convertAgendaToMap(agenda));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * MEMBER: Cast vote on agenda
     */
    @PostMapping("/agendas/{agendaId}/vote")
    public ResponseEntity<Map<String, Object>> castVote(
            @PathVariable UUID agendaId,
            @RequestBody Map<String, String> request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmailOrOfficialEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get member record
            if (user.getMemberNumber() == null) {
                throw new RuntimeException("Only members can vote");
            }

            Member member = memberRepository.findByMemberNumber(user.getMemberNumber())
                    .orElseThrow(() -> new RuntimeException("Member record not found"));

            AgendaVote.VoteChoice vote = AgendaVote.VoteChoice.valueOf(request.get("vote"));
            String comments = request.get("comments");

            AgendaVote agendaVote = meetingService.castVote(agendaId, vote, comments, member.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vote recorded successfully");
            response.put("vote", convertVoteToMap(agendaVote));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * CHAIRPERSON: Close voting for agenda
     */
    @PostMapping("/agendas/{agendaId}/close-voting")
    public ResponseEntity<Map<String, Object>> closeVoting(@PathVariable UUID agendaId) {
        try {
            MeetingAgenda agenda = meetingService.closeVoting(agendaId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voting closed successfully");
            response.put("agenda", convertAgendaToMap(agenda));
            response.put("results", Map.of(
                    "votesYes", agenda.getVotesYes(),
                    "votesNo", agenda.getVotesNo(),
                    "votesAbstain", agenda.getVotesAbstain()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * SECRETARY: Finalize agenda after voting
     */
    @PostMapping("/agendas/{agendaId}/finalize")
    public ResponseEntity<Map<String, Object>> finalizeAgenda(
            @PathVariable UUID agendaId,
            @RequestBody Map<String, String> request) {
        try {
            MeetingAgenda.AgendaDecision decision = request.get("decision") != null ?
                    MeetingAgenda.AgendaDecision.valueOf(request.get("decision")) : null;
            String decisionNotes = request.get("decisionNotes");

            MeetingAgenda agenda = meetingService.finalizeAgenda(agendaId, decision, decisionNotes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Agenda finalized successfully. Applicant has been notified.");
            response.put("agenda", convertAgendaToMap(agenda));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * SECRETARY: Close meeting
     */
    @PostMapping("/{meetingId}/close")
    public ResponseEntity<Map<String, Object>> closeMeeting(
            @PathVariable UUID meetingId,
            @RequestBody Map<String, String> request) {
        try {
            String minutesNotes = request.get("minutesNotes");
            Meeting meeting = meetingService.closeMeeting(meetingId, minutesNotes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Meeting closed successfully");
            response.put("meeting", convertMeetingToMap(meeting));
            response.put("attendance", Map.of(
                    "totalMembers", meeting.getTotalMembers(),
                    "presentMembers", meeting.getPresentMembers(),
                    "absentMembers", meeting.getAbsentMembers()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get upcoming meetings
     */
    @GetMapping("/upcoming")
    public ResponseEntity<Map<String, Object>> getUpcomingMeetings() {
        try {
            List<Meeting> meetings = meetingService.getUpcomingMeetings();
            List<Map<String, Object>> meetingsList = new ArrayList<>();

            for (Meeting meeting : meetings) {
                meetingsList.add(convertMeetingToMap(meeting));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "meetings", meetingsList
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get meeting agendas
     */
    @GetMapping("/{meetingId}/agendas")
    public ResponseEntity<Map<String, Object>> getMeetingAgendas(@PathVariable UUID meetingId) {
        try {
            List<MeetingAgenda> agendas = meetingService.getMeetingAgendas(meetingId);
            List<Map<String, Object>> agendasList = new ArrayList<>();

            for (MeetingAgenda agenda : agendas) {
                agendasList.add(convertAgendaToMap(agenda));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "agendas", agendasList
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get voting results for agenda
     */
    @GetMapping("/agendas/{agendaId}/results")
    public ResponseEntity<Map<String, Object>> getVotingResults(@PathVariable UUID agendaId) {
        try {
            Map<String, Object> results = meetingService.getVotingResults(agendaId);
            results.put("success", true);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // Helper methods to convert entities to maps
    private Map<String, Object> convertMeetingToMap(Meeting meeting) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", meeting.getId());
        map.put("meetingNumber", meeting.getMeetingNumber());
        map.put("title", meeting.getTitle());
        map.put("description", meeting.getDescription());
        map.put("meetingDate", meeting.getMeetingDate());
        map.put("meetingTime", meeting.getMeetingTime());
        map.put("venue", meeting.getVenue());
        map.put("type", meeting.getType());
        map.put("status", meeting.getStatus());
        return map;
    }

    private Map<String, Object> convertAgendaToMap(MeetingAgenda agenda) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", agenda.getId());
        map.put("agendaNumber", agenda.getAgendaNumber());
        map.put("agendaTitle", agenda.getAgendaTitle());
        map.put("agendaDescription", agenda.getAgendaDescription());
        map.put("type", agenda.getType());
        map.put("status", agenda.getStatus());
        map.put("votesYes", agenda.getVotesYes());
        map.put("votesNo", agenda.getVotesNo());
        map.put("votesAbstain", agenda.getVotesAbstain());
        map.put("decision", agenda.getDecision());
        if (agenda.getLoan() != null) {
            map.put("loanId", agenda.getLoan().getId());
            map.put("loanNumber", agenda.getLoan().getLoanNumber());
        }
        return map;
    }

    private Map<String, Object> convertVoteToMap(AgendaVote vote) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", vote.getId());
        map.put("vote", vote.getVote());
        map.put("votedAt", vote.getVotedAt());
        map.put("comments", vote.getComments());
        return map;
    }

    private UUID getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrOfficialEmail(username)
                .map(User::getId)
                .orElse(null);
    }
}

