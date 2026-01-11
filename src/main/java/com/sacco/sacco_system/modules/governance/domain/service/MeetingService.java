package com.sacco.sacco_system.modules.governance.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.governance.domain.entity.Meeting;
import com.sacco.sacco_system.modules.governance.domain.entity.MeetingLoanAgenda;
import com.sacco.sacco_system.modules.governance.domain.repository.MeetingLoanAgendaRepository;
import com.sacco.sacco_system.modules.governance.domain.repository.MeetingRepository;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing committee meetings and loan agendas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingLoanAgendaRepository agendaRepository;
    private final LoanRepository loanRepository;
    private final EmailService emailService;

    /**
     * Create a new meeting and optionally add approved loans to agenda
     */
    @Transactional
    public Meeting createMeeting(String title, Meeting.MeetingType type, LocalDate date,
                                LocalTime time, String venue, List<UUID> loanIds, String createdBy) {

        String meetingNumber = generateMeetingNumber();

        Meeting meeting = Meeting.builder()
                .meetingNumber(meetingNumber)
                .title(title)
                .meetingType(type)
                .meetingDate(date)
                .meetingTime(time)
                .venue(venue)
                .status(Meeting.MeetingStatus.SCHEDULED)
                .createdBy(createdBy)
                .attendees(new ArrayList<>())
                .build();

        Meeting savedMeeting = meetingRepository.save(meeting);
        log.info("✅ Meeting created: {} scheduled for {}", meetingNumber, date);

        // Add loans to agenda if provided
        if (loanIds != null && !loanIds.isEmpty()) {
            addLoansToAgenda(savedMeeting.getId(), loanIds, createdBy);
        }

        // Send notifications to all committee members about the meeting
        sendMeetingNotifications(savedMeeting);

        return savedMeeting;
    }

    /**
     * Send meeting notifications to all committee members
     */
    private void sendMeetingNotifications(Meeting meeting) {
        try {
            // Get loans on agenda
            List<MeetingLoanAgenda> agendaItems = agendaRepository.findByMeetingOrderByAgendaOrderAsc(meeting);

            // Build loan agenda text
            StringBuilder loanAgenda = new StringBuilder();
            for (int i = 0; i < agendaItems.size(); i++) {
                MeetingLoanAgenda item = agendaItems.get(i);
                Loan loan = item.getLoan();
                loanAgenda.append(String.format("\n%d. %s - %s %s (Member: %s)",
                    i + 1,
                    loan.getLoanNumber(),
                    loan.getProduct().getProductName(),
                    loan.getCurrencyCode() + " " + String.format("%,.2f", loan.getApprovedAmount()),
                    loan.getMember().getFirstName() + " " + loan.getMember().getLastName()
                ));
            }

            String subject = "📅 Committee Meeting Scheduled: " + meeting.getTitle();
            String message = String.format(
                "Dear Committee Member,\n\n" +
                "A committee meeting has been scheduled for loan approvals.\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "MEETING DETAILS\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Meeting No: %s\n" +
                "Title: %s\n" +
                "Date: %s\n" +
                "Time: %s\n" +
                "Venue: %s\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "LOANS ON AGENDA (%d)\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━%s\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "⚠️ IMPORTANT:\n" +
                "Please review the loan details before the meeting.\n" +
                "Your attendance and participation are crucial for decision-making.\n\n" +
                "NEXT STEPS:\n" +
                "1. Mark your calendar for the meeting\n" +
                "2. Review loan applications beforehand\n" +
                "3. Attend the meeting to vote on loan approvals\n\n" +
                "Voting will be opened by the Chairperson during or after the meeting.\n\n" +
                "Best regards,\n" +
                "SACCO Governance Department",
                meeting.getMeetingNumber(),
                meeting.getTitle(),
                meeting.getMeetingDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                meeting.getMeetingTime().format(DateTimeFormatter.ofPattern("h:mm a")),
                meeting.getVenue(),
                agendaItems.size(),
                loanAgenda.toString()
            );

            // TODO: Get all committee members from database and send email to each
            // For now, log that notifications should be sent
            log.info("📧 Meeting notifications ready to send for meeting: {}", meeting.getMeetingNumber());
            log.info("   {} loans on agenda", agendaItems.size());

            // In production, you would fetch all committee members and send emails:
            // List<Member> committeeMembers = memberRepository.findByRole("COMMITTEE");
            // for (Member member : committeeMembers) {
            //     emailService.sendEmail(member.getEmail(), subject, message);
            // }

        } catch (Exception e) {
            log.error("Failed to send meeting notifications", e);
            // Don't fail the meeting creation if notification fails
        }
    }    /**
     * Add approved loans to a meeting agenda
     */
    @Transactional
    public void addLoansToAgenda(UUID meetingId, List<UUID> loanIds, String addedBy) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ApiException("Meeting not found", 404));

        if (meeting.getStatus() != Meeting.MeetingStatus.SCHEDULED) {
            throw new ApiException("Can only add loans to scheduled meetings", 400);
        }

        int currentMaxOrder = agendaRepository.findByMeetingOrderByAgendaOrderAsc(meeting)
                .stream()
                .mapToInt(MeetingLoanAgenda::getAgendaOrder)
                .max()
                .orElse(0);

        for (UUID loanId : loanIds) {
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new ApiException("Loan not found: " + loanId, 404));

            // Verify loan is approved by loan officer
            if (loan.getLoanStatus() != Loan.LoanStatus.APPROVED) {
                throw new ApiException("Loan " + loan.getLoanNumber() + " is not approved by loan officer", 400);
            }

            // Check if loan already in this meeting's agenda
            if (agendaRepository.findByMeetingAndLoan(meeting, loan).isPresent()) {
                log.warn("Loan {} already in meeting agenda", loan.getLoanNumber());
                continue;
            }

            currentMaxOrder++;

            MeetingLoanAgenda agenda = MeetingLoanAgenda.builder()
                    .meeting(meeting)
                    .loan(loan)
                    .agendaOrder(currentMaxOrder)
                    .status(MeetingLoanAgenda.AgendaStatus.PENDING)
                    .createdBy(addedBy)
                    .build();

            agendaRepository.save(agenda);
            log.info("✅ Loan {} added to meeting {} agenda", loan.getLoanNumber(), meeting.getMeetingNumber());
        }
    }

    /**
     * Get loans awaiting committee meeting (APPROVED by loan officer, not yet scheduled)
     * Only returns loans that have NOT been added to any meeting agenda
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLoansAwaitingMeeting() {
        List<Loan> approvedLoans = loanRepository.findByLoanStatus(Loan.LoanStatus.APPROVED);

        return approvedLoans.stream()
                .filter(loan -> {
                    // Check if loan has any agenda items
                    List<MeetingLoanAgenda> agendaItems = agendaRepository.findByLoan(loan);

                    // Only include loans that have NO agenda items (never scheduled)
                    // Once a loan is added to a meeting agenda (even PENDING), it should not appear here
                    return agendaItems.isEmpty();
                })
                .map(loan -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", loan.getId());
                    data.put("loanNumber", loan.getLoanNumber());
                    data.put("memberName", loan.getMember().getFirstName() + " " + loan.getMember().getLastName());
                    data.put("memberNumber", loan.getMember().getMemberNumber());
                    data.put("productName", loan.getProduct().getProductName());
                    data.put("principalAmount", loan.getPrincipalAmount());
                    data.put("approvedAmount", loan.getApprovedAmount());
                    data.put("durationWeeks", loan.getDurationWeeks());
                    data.put("applicationDate", loan.getApplicationDate());
                    data.put("approvalDate", loan.getApprovalDate());
                    return data;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all scheduled meetings
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getScheduledMeetings() {
        List<Meeting> meetings = meetingRepository.findByStatusOrderByMeetingDateDesc(Meeting.MeetingStatus.SCHEDULED);

        return meetings.stream().map(meeting -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", meeting.getId());
            data.put("meetingNumber", meeting.getMeetingNumber());
            data.put("title", meeting.getTitle());
            data.put("meetingType", meeting.getMeetingType().name());
            data.put("meetingDate", meeting.getMeetingDate());
            data.put("meetingTime", meeting.getMeetingTime());
            data.put("venue", meeting.getVenue());
            data.put("status", meeting.getStatus().name());

            // Get loan count for this meeting
            List<MeetingLoanAgenda> agendas = agendaRepository.findByMeetingOrderByAgendaOrderAsc(meeting);
            data.put("loanCount", agendas.size());

            return data;
        }).collect(Collectors.toList());
    }

    /**
     * Get all meetings regardless of status (for dashboards)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllMeetings() {
        List<Meeting> meetings = meetingRepository.findAll();

        return meetings.stream()
                .sorted((m1, m2) -> m2.getMeetingDate().compareTo(m1.getMeetingDate()))
                .map(meeting -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", meeting.getId());
                    data.put("meetingNumber", meeting.getMeetingNumber());
                    data.put("title", meeting.getTitle());
                    data.put("meetingType", meeting.getMeetingType().name());
                    data.put("meetingDate", meeting.getMeetingDate());
                    data.put("meetingTime", meeting.getMeetingTime());
                    data.put("venue", meeting.getVenue());
                    data.put("status", meeting.getStatus().name());
                    data.put("minutes", meeting.getMinutes()); // ✅ Include minutes

                    // Get loan count for this meeting
                    List<MeetingLoanAgenda> agendas = agendaRepository.findByMeetingOrderByAgendaOrderAsc(meeting);
                    data.put("loanCount", agendas.size());

                    return data;
                }).collect(Collectors.toList());
    }

    /**
     * Get meeting details with agenda
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMeetingWithAgenda(UUID meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ApiException("Meeting not found", 404));

        Map<String, Object> data = new HashMap<>();
        data.put("id", meeting.getId());
        data.put("meetingNumber", meeting.getMeetingNumber());
        data.put("title", meeting.getTitle());
        data.put("meetingType", meeting.getMeetingType().name());
        data.put("meetingDate", meeting.getMeetingDate());
        data.put("meetingTime", meeting.getMeetingTime());
        data.put("venue", meeting.getVenue());
        data.put("status", meeting.getStatus().name());
        data.put("agenda", meeting.getAgenda());
        data.put("minutes", meeting.getMinutes());

        // Get agenda items (loans)
        List<MeetingLoanAgenda> agendas = agendaRepository.findByMeetingOrderByAgendaOrderAsc(meeting);
        List<Map<String, Object>> agendaItems = agendas.stream().map(agenda -> {
            Loan loan = agenda.getLoan();
            Map<String, Object> item = new HashMap<>();
            item.put("agendaId", agenda.getId());
            item.put("agendaOrder", agenda.getAgendaOrder());
            item.put("loanId", loan.getId());
            item.put("loanNumber", loan.getLoanNumber());
            item.put("memberName", loan.getMember().getFirstName() + " " + loan.getMember().getLastName());
            item.put("memberNumber", loan.getMember().getMemberNumber());
            item.put("productName", loan.getProduct().getProductName());
            item.put("principalAmount", loan.getPrincipalAmount());
            item.put("approvedAmount", loan.getApprovedAmount());
            item.put("durationWeeks", loan.getDurationWeeks());
            item.put("agendaStatus", agenda.getStatus().name());
            item.put("notes", agenda.getNotes());
            item.put("discussion", agenda.getDiscussion());
            item.put("decision", agenda.getDecision());
            return item;
        }).collect(Collectors.toList());

        data.put("agendaItems", agendaItems);

        return data;
    }

    /**
     * Generate unique meeting number
     */
    private String generateMeetingNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "MTG-" + datePart + "-" + random;
    }

    /**
     * Update meeting details (only for SCHEDULED meetings)
     */
    @Transactional
    public void updateMeeting(UUID meetingId, String title, LocalDate date, LocalTime time, String venue, String updatedBy) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ApiException("Meeting not found", 404));

        // Only allow updates for scheduled meetings
        if (meeting.getStatus() != Meeting.MeetingStatus.SCHEDULED) {
            throw new ApiException("Cannot update meeting that has already started or completed", 400);
        }

        if (title != null && !title.isEmpty()) {
            meeting.setTitle(title);
        }
        if (date != null) {
            meeting.setMeetingDate(date);
        }
        if (time != null) {
            meeting.setMeetingTime(time);
        }
        if (venue != null && !venue.isEmpty()) {
            meeting.setVenue(venue);
        }

        meeting.setUpdatedBy(updatedBy);
        meetingRepository.save(meeting);

        log.info("✅ Meeting {} updated by {}", meeting.getMeetingNumber(), updatedBy);
    }

    /**
     * Update meeting minutes (can be called by secretary to edit auto-generated or write custom minutes)
     */
    @Transactional
    public void updateMinutes(UUID meetingId, String minutes, String updatedBy) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ApiException("Meeting not found", 404));

        meeting.setMinutes(minutes);
        meeting.setUpdatedBy(updatedBy);
        meetingRepository.save(meeting);

        log.info("✅ Minutes updated for meeting {} by {}", meeting.getMeetingNumber(), updatedBy);
    }

    /**
     * Update meeting status
     */
    @Transactional
    public void updateMeetingStatus(UUID meetingId, Meeting.MeetingStatus status, String updatedBy) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ApiException("Meeting not found", 404));

        meeting.setStatus(status);
        meeting.setUpdatedBy(updatedBy);
        meetingRepository.save(meeting);

        log.info("✅ Meeting {} status updated to {}", meeting.getMeetingNumber(), status);
    }
}

