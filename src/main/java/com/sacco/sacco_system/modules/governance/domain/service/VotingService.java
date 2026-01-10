package com.sacco.sacco_system.modules.governance.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.governance.domain.entity.Meeting;
import com.sacco.sacco_system.modules.governance.domain.entity.MeetingLoanAgenda;
import com.sacco.sacco_system.modules.governance.domain.entity.MeetingLoanVote;
import com.sacco.sacco_system.modules.governance.domain.repository.MeetingLoanAgendaRepository;
import com.sacco.sacco_system.modules.governance.domain.repository.MeetingLoanVoteRepository;
import com.sacco.sacco_system.modules.governance.domain.repository.MeetingRepository;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing committee voting on loans
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VotingService {

    private final MeetingRepository meetingRepository;
    private final MeetingLoanAgendaRepository agendaRepository;
    private final MeetingLoanVoteRepository voteRepository;
    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;

    /**
     * Chairperson opens voting for a meeting
     * Can only open if meeting date/time has passed or is current
     */
    @Transactional
    public void openVoting(UUID meetingId, String chairpersonEmail) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ApiException("Meeting not found", 404));

        // Verify meeting is scheduled
        if (meeting.getStatus() != Meeting.MeetingStatus.SCHEDULED) {
            throw new ApiException("Can only open voting for scheduled meetings", 400);
        }

        // Check if meeting date/time has passed or is current
        LocalDateTime meetingDateTime = LocalDateTime.of(meeting.getMeetingDate(), meeting.getMeetingTime());
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(meetingDateTime)) {
            throw new ApiException(
                String.format("Cannot open voting before meeting time. Meeting scheduled for %s at %s",
                    meeting.getMeetingDate(), meeting.getMeetingTime()),
                400
            );
        }

        // Change meeting status to IN_PROGRESS
        meeting.setStatus(Meeting.MeetingStatus.IN_PROGRESS);
        meeting.setChairperson(chairpersonEmail);
        meetingRepository.save(meeting);

        log.info("✅ Voting opened for meeting {} by chairperson {}", meeting.getMeetingNumber(), chairpersonEmail);
    }

    /**
     * Committee member votes on a loan agenda item
     */
    @Transactional
    public void castVote(UUID agendaItemId, String voterEmail, MeetingLoanVote.VoteDecision decision, String comments) {
        MeetingLoanAgenda agendaItem = agendaRepository.findById(agendaItemId)
                .orElseThrow(() -> new ApiException("Agenda item not found", 404));

        Meeting meeting = agendaItem.getMeeting();

        // ✅ Check if voting is still open
        if (meeting.getStatus() != Meeting.MeetingStatus.IN_PROGRESS) {
            if (meeting.getStatus() == Meeting.MeetingStatus.VOTING_CLOSED) {
                throw new ApiException("Voting has been closed by the chairperson. No more votes can be cast.", 400);
            } else if (meeting.getStatus() == Meeting.MeetingStatus.COMPLETED) {
                throw new ApiException("This meeting has been completed and finalized.", 400);
            } else {
                throw new ApiException("Voting is not open for this meeting", 400);
            }
        }

        // Get voter
        Member voter = memberRepository.findByEmail(voterEmail)
                .orElseThrow(() -> new ApiException("Voter not found", 404));

        // Check if already voted
        if (voteRepository.existsByAgendaItemAndVoter(agendaItem, voter)) {
            throw new ApiException("You have already voted on this loan", 400);
        }

        // Cast vote
        MeetingLoanVote vote = MeetingLoanVote.builder()
                .agendaItem(agendaItem)
                .voter(voter)
                .decision(decision)
                .comments(comments)
                .build();

        voteRepository.save(vote);

        log.info("✅ Vote cast: {} voted {} on loan {} in meeting {}",
                voter.getMemberNumber(), decision, agendaItem.getLoan().getLoanNumber(), meeting.getMeetingNumber());

        // Check if all agenda items have been voted on (auto-close voting if complete)
        checkAndCompleteVoting(meeting);
    }

    /**
     * Get voting results for a meeting
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getVotingResults(UUID meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ApiException("Meeting not found", 404));

        List<MeetingLoanAgenda> agendaItems = agendaRepository.findByMeetingOrderByAgendaOrderAsc(meeting);

        List<Map<String, Object>> results = agendaItems.stream().map(agendaItem -> {
            Map<String, Object> result = new HashMap<>();
            result.put("agendaId", agendaItem.getId());
            result.put("loanNumber", agendaItem.getLoan().getLoanNumber());
            result.put("memberName", agendaItem.getLoan().getMember().getFirstName() + " " +
                                    agendaItem.getLoan().getMember().getLastName());
            result.put("amount", agendaItem.getLoan().getApprovedAmount());

            // Count votes
            long totalVotes = voteRepository.countByAgendaItem(agendaItem);
            long approveVotes = voteRepository.countByAgendaItemAndDecision(agendaItem, MeetingLoanVote.VoteDecision.APPROVE);
            long rejectVotes = voteRepository.countByAgendaItemAndDecision(agendaItem, MeetingLoanVote.VoteDecision.REJECT);
            long abstainVotes = voteRepository.countByAgendaItemAndDecision(agendaItem, MeetingLoanVote.VoteDecision.ABSTAIN);
            long deferVotes = voteRepository.countByAgendaItemAndDecision(agendaItem, MeetingLoanVote.VoteDecision.DEFER);

            result.put("totalVotes", totalVotes);
            result.put("approveVotes", approveVotes);
            result.put("rejectVotes", rejectVotes);
            result.put("abstainVotes", abstainVotes);
            result.put("deferVotes", deferVotes);

            // Determine outcome
            String outcome = "PENDING";
            if (totalVotes > 0) {
                if (approveVotes > rejectVotes) {
                    outcome = "APPROVED";
                } else if (rejectVotes > approveVotes) {
                    outcome = "REJECTED";
                } else {
                    outcome = "TIED";
                }
            }
            result.put("outcome", outcome);
            result.put("agendaStatus", agendaItem.getStatus().name());

            return result;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("meetingNumber", meeting.getMeetingNumber());
        response.put("meetingStatus", meeting.getStatus().name());
        response.put("totalAgendaItems", agendaItems.size());
        response.put("totalVotesCast", voteRepository.countVotesByMeetingId(meetingId));
        response.put("results", results);

        return response;
    }

    /**
     * Check if all agenda items have sufficient votes and complete meeting if done
     */
    private void checkAndCompleteVoting(Meeting meeting) {
        List<MeetingLoanAgenda> agendaItems = agendaRepository.findByMeetingOrderByAgendaOrderAsc(meeting);

        // Check if all items have at least one vote (simplified logic)
        boolean allVoted = agendaItems.stream()
                .allMatch(item -> voteRepository.countByAgendaItem(item) > 0);

        if (allVoted) {
            log.info("🎉 All agenda items have been voted on for meeting {}", meeting.getMeetingNumber());
            // Could auto-complete here or wait for chairperson to close
        }
    }

    /**
     * Chairperson closes voting - no more votes can be cast
     * Results are NOT finalized yet - Secretary does that
     */
    @Transactional
    public void closeVoting(UUID meetingId, String chairpersonEmail) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ApiException("Meeting not found", 404));

        if (meeting.getStatus() != Meeting.MeetingStatus.IN_PROGRESS) {
            throw new ApiException("Voting is not open for this meeting", 400);
        }

        // ✅ Change status to VOTING_CLOSED (not COMPLETED)
        // This prevents new votes but allows secretary to finalize
        meeting.setStatus(Meeting.MeetingStatus.VOTING_CLOSED);
        meetingRepository.save(meeting);

        log.info("✅ Voting closed for meeting {} by chairperson {} - Awaiting secretary to finalize results",
                meeting.getMeetingNumber(), chairpersonEmail);
    }

    /**
     * Secretary finalizes voting results, generates minutes, and forwards loans for disbursement
     */
    @Transactional
    public void finalizeVotingResults(UUID meetingId, String secretaryEmail) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ApiException("Meeting not found", 404));

        if (meeting.getStatus() != Meeting.MeetingStatus.VOTING_CLOSED) {
            throw new ApiException("Voting must be closed before finalizing results", 400);
        }

        // Update meeting status to COMPLETED
        meeting.setStatus(Meeting.MeetingStatus.COMPLETED);
        meetingRepository.save(meeting);

        // Update loan statuses based on voting results
        List<MeetingLoanAgenda> agendaItems = agendaRepository.findByMeetingOrderByAgendaOrderAsc(meeting);

        StringBuilder minutesBuilder = new StringBuilder();
        minutesBuilder.append("COMMITTEE MEETING MINUTES\n");
        minutesBuilder.append("=========================\n\n");
        minutesBuilder.append("Meeting: ").append(meeting.getTitle()).append("\n");
        minutesBuilder.append("Meeting Number: ").append(meeting.getMeetingNumber()).append("\n");
        minutesBuilder.append("Date: ").append(meeting.getMeetingDate()).append("\n");
        minutesBuilder.append("Time: ").append(meeting.getMeetingTime()).append("\n");
        minutesBuilder.append("Venue: ").append(meeting.getVenue()).append("\n\n");
        minutesBuilder.append("LOAN APPLICATIONS REVIEW\n");
        minutesBuilder.append("========================\n\n");

        int agendaNumber = 1;
        for (MeetingLoanAgenda agendaItem : agendaItems) {
            long approveVotes = voteRepository.countByAgendaItemAndDecision(agendaItem, MeetingLoanVote.VoteDecision.APPROVE);
            long rejectVotes = voteRepository.countByAgendaItemAndDecision(agendaItem, MeetingLoanVote.VoteDecision.REJECT);
            long abstainVotes = voteRepository.countByAgendaItemAndDecision(agendaItem, MeetingLoanVote.VoteDecision.ABSTAIN);
            long deferVotes = voteRepository.countByAgendaItemAndDecision(agendaItem, MeetingLoanVote.VoteDecision.DEFER);

            String decision;
            MeetingLoanAgenda.AgendaStatus status;
            Loan.LoanStatus loanStatus;

            if (approveVotes > rejectVotes) {
                status = MeetingLoanAgenda.AgendaStatus.APPROVED;
                loanStatus = Loan.LoanStatus.APPROVED_BY_COMMITTEE;
                decision = "APPROVED by committee vote";
            } else if (rejectVotes > approveVotes) {
                status = MeetingLoanAgenda.AgendaStatus.REJECTED;
                loanStatus = Loan.LoanStatus.REJECTED;
                decision = "REJECTED by committee vote";
            } else {
                status = MeetingLoanAgenda.AgendaStatus.DEFERRED;
                loanStatus = Loan.LoanStatus.UNDER_REVIEW; // Back to review
                decision = "DEFERRED - tied vote";
            }

            // Update agenda item
            agendaItem.setStatus(status);
            agendaItem.setDecision(decision + " (" + approveVotes + " approve, " + rejectVotes + " reject, " +
                    abstainVotes + " abstain, " + deferVotes + " defer)");
            agendaRepository.save(agendaItem);

            // Update loan status
            Loan loan = agendaItem.getLoan();
            loan.setLoanStatus(loanStatus);
            loanRepository.save(loan);

            // Add to minutes
            minutesBuilder.append(agendaNumber++).append(". ");
            minutesBuilder.append("Loan Application: ").append(loan.getLoanNumber()).append("\n");
            minutesBuilder.append("   Applicant: ")
                    .append(loan.getMember().getFirstName()).append(" ")
                    .append(loan.getMember().getLastName())
                    .append(" (").append(loan.getMember().getMemberNumber()).append(")\n");
            minutesBuilder.append("   Amount: KES ").append(loan.getApprovedAmount()).append("\n");
            minutesBuilder.append("   Product: ").append(loan.getProduct().getProductName()).append("\n");
            minutesBuilder.append("   Voting Results:\n");
            minutesBuilder.append("     - Approve: ").append(approveVotes).append("\n");
            minutesBuilder.append("     - Reject: ").append(rejectVotes).append("\n");
            minutesBuilder.append("     - Abstain: ").append(abstainVotes).append("\n");
            minutesBuilder.append("     - Defer: ").append(deferVotes).append("\n");
            minutesBuilder.append("   DECISION: ").append(decision).append("\n\n");

            log.info("✅ Loan {} - {}", loan.getLoanNumber(), decision);
        }

        // Save minutes to meeting
        meeting.setMinutes(minutesBuilder.toString());
        meetingRepository.save(meeting);

        log.info("✅ Voting results finalized for meeting {} by secretary {}. Minutes generated. {} loans forwarded for disbursement.",
                meeting.getMeetingNumber(), secretaryEmail,
                agendaItems.stream().filter(a -> a.getStatus() == MeetingLoanAgenda.AgendaStatus.APPROVED).count());
    }

    /**
     * Get loans available for voting (meeting is IN_PROGRESS)
     * Excludes member's own loans - they cannot vote on their own applications
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLoansForVoting(String voterEmail) {
        // Find meetings that are IN_PROGRESS
        List<Meeting> activeMeetings = meetingRepository.findByStatusOrderByMeetingDateDesc(Meeting.MeetingStatus.IN_PROGRESS);

        List<Map<String, Object>> availableLoans = new ArrayList<>();

        Member voter = memberRepository.findByEmail(voterEmail)
                .orElseThrow(() -> new ApiException("Voter not found", 404));

        for (Meeting meeting : activeMeetings) {
            List<MeetingLoanAgenda> agendaItems = agendaRepository.findByMeetingOrderByAgendaOrderAsc(meeting);

            for (MeetingLoanAgenda agendaItem : agendaItems) {
                Loan loan = agendaItem.getLoan();

                // ✅ EXCLUDE member's own loan - cannot vote on their own application
                if (loan.getMember().getId().equals(voter.getId())) {
                    log.debug("Excluding own loan {} from voting for member {}",
                            loan.getLoanNumber(), voter.getMemberNumber());
                    continue; // Skip this loan
                }

                // Check if voter has already voted
                boolean hasVoted = voteRepository.existsByAgendaItemAndVoter(agendaItem, voter);

                Map<String, Object> loanData = new HashMap<>();
                loanData.put("agendaItemId", agendaItem.getId());
                loanData.put("meetingNumber", meeting.getMeetingNumber());
                loanData.put("loanNumber", loan.getLoanNumber());
                loanData.put("memberName", loan.getMember().getFirstName() + " " +
                                          loan.getMember().getLastName());
                loanData.put("memberNumber", loan.getMember().getMemberNumber());
                loanData.put("productName", loan.getProduct().getProductName());
                loanData.put("principalAmount", loan.getPrincipalAmount());
                loanData.put("approvedAmount", loan.getApprovedAmount());
                loanData.put("durationWeeks", loan.getDurationWeeks());
                loanData.put("hasVoted", hasVoted);

                availableLoans.add(loanData);
            }
        }

        return availableLoans;
    }
}

