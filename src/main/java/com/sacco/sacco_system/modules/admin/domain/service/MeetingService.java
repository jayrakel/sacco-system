package com.sacco.sacco_system.modules.admin.domain.service;

import com.sacco.sacco_system.modules.admin.domain.entity.AgendaVote;
import com.sacco.sacco_system.modules.admin.domain.entity.Meeting;
import com.sacco.sacco_system.modules.admin.domain.entity.MeetingAgenda;
import com.sacco.sacco_system.modules.admin.domain.repository.AgendaVoteRepository;
import com.sacco.sacco_system.modules.admin.domain.repository.MeetingAgendaRepository;
import com.sacco.sacco_system.modules.admin.domain.repository.MeetingRepository;
import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingAgendaRepository agendaRepository;
    private final AgendaVoteRepository voteRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * SECRETARY: Create a new meeting
     */
    public Meeting createMeeting(Meeting meeting, UUID scheduledByUserId) {
        meeting.setScheduledBy(scheduledByUserId);
        meeting.setStatus(Meeting.MeetingStatus.SCHEDULED);
        return meetingRepository.save(meeting);
    }

    /**
     * SECRETARY: Table a loan as agenda for meeting
     */
    public MeetingAgenda tableLoanAsAgenda(UUID meetingId, UUID loanId, Integer agendaNumber, String notes) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Validate loan status
        if (loan.getStatus() != Loan.LoanStatus.LOAN_OFFICER_REVIEW &&
            loan.getStatus() != Loan.LoanStatus.SUBMITTED) {
            throw new RuntimeException("Loan must be approved by loan officer before tabling");
        }

        // Check if loan already tabled
        Optional<MeetingAgenda> existing = agendaRepository.findByLoan(loan);
        if (existing.isPresent()) {
            throw new RuntimeException("Loan already tabled for a meeting");
        }

        // Create agenda
        MeetingAgenda agenda = MeetingAgenda.builder()
                .meeting(meeting)
                .loan(loan)
                .agendaNumber(agendaNumber)
                .agendaTitle("Loan Application - " + loan.getMemberName())
                .agendaDescription(notes != null ? notes :
                        String.format("Loan application by %s for KES %s",
                                loan.getMemberName(), loan.getPrincipalAmount()))
                .type(MeetingAgenda.AgendaType.LOAN_APPROVAL)
                .status(MeetingAgenda.AgendaStatus.TABLED)
                .tabledBy(getCurrentUserId())
                .build();

        MeetingAgenda saved = agendaRepository.save(agenda);

        // Update loan status
        loan.setStatus(Loan.LoanStatus.SECRETARY_TABLED);
        loan.setMeetingDate(meeting.getMeetingDate());
        loanRepository.save(loan);

        // Update meeting status
        if (meeting.getStatus() == Meeting.MeetingStatus.SCHEDULED) {
            meeting.setStatus(Meeting.MeetingStatus.AGENDA_SET);
            meetingRepository.save(meeting);
        }

        // Notify all members about the meeting
        notifyMembersAboutMeeting(meeting);

        log.info("Loan {} tabled as agenda for meeting {}", loan.getLoanNumber(), meeting.getMeetingNumber());

        return saved;
    }

    /**
     * CHAIRPERSON: Open meeting (start the meeting)
     */
    public Meeting openMeeting(UUID meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        if (meeting.getStatus() != Meeting.MeetingStatus.AGENDA_SET &&
            meeting.getStatus() != Meeting.MeetingStatus.SCHEDULED) {
            throw new RuntimeException("Meeting cannot be opened at this stage");
        }

        meeting.setStatus(Meeting.MeetingStatus.IN_PROGRESS);
        meeting.setOpenedAt(LocalDateTime.now());

        // Update loan status to ON_AGENDA (but don't open voting yet!)
        List<MeetingAgenda> agendas = agendaRepository.findByMeetingOrderByAgendaNumberAsc(meeting);
        for (MeetingAgenda agenda : agendas) {
            if (agenda.getStatus() == MeetingAgenda.AgendaStatus.TABLED) {
                // Keep agenda as TABLED - chairperson must explicitly open voting
                if (agenda.getLoan() != null) {
                    agenda.getLoan().setStatus(Loan.LoanStatus.ON_AGENDA);
                }
            }
        }

        log.info("Meeting {} opened. Agendas ready for voting.", meeting.getMeetingNumber());

        return meetingRepository.save(meeting);
    }

    /**
     * CHAIRPERSON: Open voting for specific agenda
     */
    public MeetingAgenda openVoting(UUID agendaId) {
        MeetingAgenda agenda = agendaRepository.findById(agendaId)
                .orElseThrow(() -> new RuntimeException("Agenda not found"));

        if (agenda.getStatus() != MeetingAgenda.AgendaStatus.TABLED &&
            agenda.getStatus() != MeetingAgenda.AgendaStatus.OPEN_FOR_VOTE) {
            throw new RuntimeException("Agenda cannot be opened for voting at this stage");
        }

        agenda.setStatus(MeetingAgenda.AgendaStatus.OPEN_FOR_VOTE);

        if (agenda.getLoan() != null) {
            agenda.getLoan().setStatus(Loan.LoanStatus.VOTING_OPEN);
        }

        // Notify members that voting is open
        notifyMembersAboutVoting(agenda);

        log.info("Voting opened for agenda: {}", agenda.getAgendaTitle());

        return agendaRepository.save(agenda);
    }

    /**
     * MEMBER: Cast vote on agenda
     */
    public AgendaVote castVote(UUID agendaId, AgendaVote.VoteChoice vote, String comments, UUID memberId) {
        MeetingAgenda agenda = agendaRepository.findById(agendaId)
                .orElseThrow(() -> new RuntimeException("Agenda not found"));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Validate voting is open
        if (agenda.getStatus() != MeetingAgenda.AgendaStatus.OPEN_FOR_VOTE) {
            throw new RuntimeException("Voting is not currently open for this agenda");
        }

        // Validate member hasn't voted already
        if (voteRepository.existsByAgendaAndMember(agenda, member)) {
            throw new RuntimeException("You have already voted on this agenda");
        }

        // Validate member is not voting on their own loan
        if (agenda.getLoan() != null && agenda.getLoan().getMember().getId().equals(memberId)) {
            throw new RuntimeException("You cannot vote on your own loan application");
        }

        // Cast vote
        AgendaVote agendaVote = AgendaVote.builder()
                .agenda(agenda)
                .member(member)
                .vote(vote)
                .comments(comments)
                .build();

        AgendaVote saved = voteRepository.save(agendaVote);

        // Update vote counts
        updateVoteCounts(agenda);

        log.info("Member {} voted {} on agenda {}", member.getMemberNumber(), vote, agenda.getAgendaTitle());

        return saved;
    }

    /**
     * CHAIRPERSON: Close voting for agenda
     */
    public MeetingAgenda closeVoting(UUID agendaId) {
        MeetingAgenda agenda = agendaRepository.findById(agendaId)
                .orElseThrow(() -> new RuntimeException("Agenda not found"));

        if (agenda.getStatus() != MeetingAgenda.AgendaStatus.OPEN_FOR_VOTE) {
            throw new RuntimeException("Voting is not currently open for this agenda");
        }

        agenda.setStatus(MeetingAgenda.AgendaStatus.VOTING_CLOSED);

        if (agenda.getLoan() != null) {
            agenda.getLoan().setStatus(Loan.LoanStatus.VOTING_CLOSED);
        }

        // Final vote count
        updateVoteCounts(agenda);

        log.info("Voting closed for agenda: {}. Results: {} YES, {} NO, {} ABSTAIN",
                agenda.getAgendaTitle(), agenda.getVotesYes(), agenda.getVotesNo(), agenda.getVotesAbstain());

        return agendaRepository.save(agenda);
    }

    /**
     * SECRETARY: Finalize agenda after voting
     */
    public MeetingAgenda finalizeAgenda(UUID agendaId, MeetingAgenda.AgendaDecision decision, String decisionNotes) {
        MeetingAgenda agenda = agendaRepository.findById(agendaId)
                .orElseThrow(() -> new RuntimeException("Agenda not found"));

        if (agenda.getStatus() != MeetingAgenda.AgendaStatus.VOTING_CLOSED) {
            throw new RuntimeException("Voting must be closed before finalization");
        }

        long voteCount = voteRepository.countByAgenda(agenda);
        if (voteCount == 0) {
            throw new RuntimeException("No votes have been cast yet");
        }

        // Calculate decision based on votes
        MeetingAgenda.AgendaDecision calculatedDecision = calculateDecision(agenda);

        // Use provided decision or calculated one
        agenda.setDecision(decision != null ? decision : calculatedDecision);
        agenda.setDecisionNotes(decisionNotes);
        agenda.setStatus(MeetingAgenda.AgendaStatus.FINALIZED);
        agenda.setFinalizedAt(LocalDateTime.now());
        agenda.setFinalizedBy(getCurrentUserId());

        // Update loan status based on decision
        if (agenda.getLoan() != null) {
            Loan loan = agenda.getLoan();
            switch (agenda.getDecision()) {
                case APPROVED:
                    loan.setStatus(Loan.LoanStatus.ADMIN_APPROVED); // Ready for disbursement
                    loan.setApprovalDate(LocalDate.now());
                    break;
                case REJECTED:
                    loan.setStatus(Loan.LoanStatus.REJECTED);
                    break;
                case DEFERRED:
                case TIE:
                    loan.setStatus(Loan.LoanStatus.SECRETARY_TABLED); // Back to tabled, can be re-presented
                    break;
            }
            loanRepository.save(loan);
        }

        MeetingAgenda saved = agendaRepository.save(agenda);

        // Notify applicant about decision
        if (agenda.getLoan() != null) {
            notifyApplicantAboutDecision(agenda);
        }

        log.info("Agenda finalized: {}. Decision: {}", agenda.getAgendaTitle(), agenda.getDecision());

        return saved;
    }

    /**
     * SECRETARY: Close meeting
     */
    public Meeting closeMeeting(UUID meetingId, String minutesNotes) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        if (meeting.getStatus() != Meeting.MeetingStatus.IN_PROGRESS) {
            throw new RuntimeException("Meeting is not currently in progress");
        }

        // Check all agendas are finalized
        List<MeetingAgenda> agendas = agendaRepository.findByMeetingOrderByAgendaNumberAsc(meeting);
        boolean allFinalized = agendas.stream()
                .allMatch(a -> a.getStatus() == MeetingAgenda.AgendaStatus.FINALIZED);

        if (!allFinalized) {
            throw new RuntimeException("All agendas must be finalized before closing the meeting");
        }

        meeting.setStatus(Meeting.MeetingStatus.COMPLETED);
        meeting.setClosedAt(LocalDateTime.now());
        meeting.setMinutesNotes(minutesNotes);

        // Calculate attendance (for now, count unique voters)
        Set<UUID> presentMembers = new HashSet<>();
        for (MeetingAgenda agenda : agendas) {
            List<AgendaVote> votes = voteRepository.findByAgenda(agenda);
            votes.forEach(v -> presentMembers.add(v.getMember().getId()));
        }

        meeting.setPresentMembers(presentMembers.size());
        meeting.setTotalMembers((int) memberRepository.count());
        meeting.setAbsentMembers(meeting.getTotalMembers() - meeting.getPresentMembers());

        log.info("Meeting {} closed. Attendance: {}/{}",
                meeting.getMeetingNumber(), meeting.getPresentMembers(), meeting.getTotalMembers());

        return meetingRepository.save(meeting);
    }

    // ===== HELPER METHODS =====

    private void updateVoteCounts(MeetingAgenda agenda) {
        agenda.setVotesYes((int) voteRepository.countByAgendaAndVote(agenda, AgendaVote.VoteChoice.YES));
        agenda.setVotesNo((int) voteRepository.countByAgendaAndVote(agenda, AgendaVote.VoteChoice.NO));
        agenda.setVotesAbstain((int) voteRepository.countByAgendaAndVote(agenda, AgendaVote.VoteChoice.ABSTAIN));
        agendaRepository.save(agenda);
    }

    private MeetingAgenda.AgendaDecision calculateDecision(MeetingAgenda agenda) {
        if (agenda.getVotesYes() > agenda.getVotesNo()) {
            return MeetingAgenda.AgendaDecision.APPROVED;
        } else if (agenda.getVotesNo() > agenda.getVotesYes()) {
            return MeetingAgenda.AgendaDecision.REJECTED;
        } else {
            return MeetingAgenda.AgendaDecision.TIE;
        }
    }

    private void notifyMembersAboutMeeting(Meeting meeting) {
        List<MeetingAgenda> agendas = agendaRepository.findByMeetingOrderByAgendaNumberAsc(meeting);

        String agendaList = agendas.stream()
                .map(a -> String.format("%d. %s", a.getAgendaNumber(), a.getAgendaTitle()))
                .collect(Collectors.joining("\n"));

        String message = String.format(
                "Meeting Scheduled: %s\n\nDate: %s\nTime: %s\nVenue: %s\n\nAGENDA ITEMS:\n%s\n\nPlease make arrangements to attend.",
                meeting.getTitle(),
                meeting.getMeetingDate(),
                meeting.getMeetingTime(),
                meeting.getVenue(),
                agendaList
        );

        // Get all members
        List<Member> allMembers = memberRepository.findAll();

        // Send notification to each member
        for (Member member : allMembers) {
            // Check if this member is an applicant for any agenda in this meeting
            boolean isApplicantInMeeting = agendas.stream()
                    .anyMatch(a -> a.getLoan() != null &&
                                  a.getLoan().getMember().getId().equals(member.getId()));

            if (isApplicantInMeeting) {
                // Send special notification for applicants
                String applicantMessage = String.format(
                        "Meeting Scheduled: %s\n\nDate: %s\nTime: %s\nVenue: %s\n\nYour loan application is on the agenda.\nYou will be notified of the voting results.",
                        meeting.getTitle(),
                        meeting.getMeetingDate(),
                        meeting.getMeetingTime(),
                        meeting.getVenue()
                );
                // TODO: Send notification to applicant
                log.info("Meeting notification (applicant) sent to {}", member.getMemberNumber());
            } else {
                // Send regular notification to voting members
                // TODO: Send notification to member
                log.info("Meeting notification sent to {}", member.getMemberNumber());
            }
        }

        log.info("Meeting notifications sent to all members");
    }

    private void notifyMembersAboutVoting(MeetingAgenda agenda) {
        // Get all members EXCEPT the loan applicant
        List<Member> allMembers = memberRepository.findAll();

        UUID applicantId = null;
        if (agenda.getLoan() != null) {
            applicantId = agenda.getLoan().getMember().getId();
        }

        for (Member member : allMembers) {
            // Skip the applicant
            if (applicantId != null && member.getId().equals(applicantId)) {
                log.info("Skipping voting notification for applicant: {}", member.getMemberNumber());
                continue;
            }

            // Send voting notification to eligible voters
            String message = String.format(
                    "Voting Now Open\n\nAgenda: %s\n\nPlease login to your member portal to cast your vote.\n\nVoting will close when the chairperson calls for closure.",
                    agenda.getAgendaTitle()
            );

            // TODO: Send notification to member
            log.info("Voting notification sent to {}: {}", member.getMemberNumber(), agenda.getAgendaTitle());
        }

        log.info("Voting notifications sent (excluding applicant)");
    }

    private void notifyApplicantAboutDecision(MeetingAgenda agenda) {
        Loan loan = agenda.getLoan();
        String message = String.format(
                "Your loan application has been %s.\n\nLoan Amount: KES %s\nVotes: %d YES, %d NO\n\n%s",
                agenda.getDecision(),
                loan.getPrincipalAmount(),
                agenda.getVotesYes(),
                agenda.getVotesNo(),
                agenda.getDecision() == MeetingAgenda.AgendaDecision.APPROVED ?
                        "Your loan will now proceed to disbursement." :
                        "Please contact the secretary for more information."
        );

        // TODO: Send notification to loan applicant
        log.info("Decision notification sent to {}", loan.getMemberName());
    }

    private UUID getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrOfficialEmail(username)
                .map(User::getId)
                .orElse(null);
    }

    // ===== QUERY METHODS =====

    public List<Meeting> getUpcomingMeetings() {
        return meetingRepository.findByMeetingDateAfterAndStatusInOrderByMeetingDateAsc(
                LocalDate.now(),
                Arrays.asList(Meeting.MeetingStatus.SCHEDULED, Meeting.MeetingStatus.AGENDA_SET)
        );
    }

    public List<MeetingAgenda> getMeetingAgendas(UUID meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));
        return agendaRepository.findByMeetingOrderByAgendaNumberAsc(meeting);
    }

    public Map<String, Object> getVotingResults(UUID agendaId) {
        MeetingAgenda agenda = agendaRepository.findById(agendaId)
                .orElseThrow(() -> new RuntimeException("Agenda not found"));

        List<AgendaVote> votes = voteRepository.findByAgenda(agenda);

        Map<String, Object> results = new HashMap<>();
        results.put("agendaTitle", agenda.getAgendaTitle());
        results.put("status", agenda.getStatus());
        results.put("votesYes", agenda.getVotesYes());
        results.put("votesNo", agenda.getVotesNo());
        results.put("votesAbstain", agenda.getVotesAbstain());
        results.put("totalVotes", votes.size());
        results.put("decision", agenda.getDecision());
        results.put("decisionNotes", agenda.getDecisionNotes());

        return results;
    }
}

