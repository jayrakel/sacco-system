package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.notification.domain.service.NotificationService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanGovernanceService {

    private final LoanRepository loanRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // --- LOAN OFFICER ---
    @Transactional
    public void reviewApplication(UUID loanId, String decision, String remarks) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if ("APPROVE".equalsIgnoreCase(decision)) {
            loan.setStatus(Loan.LoanStatus.VERIFIED);
            loan.setApprovalDate(LocalDate.now());

            notificationService.notifyUser(loan.getMember().getId(), "Application Verified",
                    "Your loan has passed initial review and is awaiting committee scheduling.", true, false);
        } else {
            loan.setStatus(Loan.LoanStatus.REJECTED);
            loan.setRejectionReason(remarks);
            notificationService.notifyUser(loan.getMember().getId(), "Application Rejected",
                    "Reason: " + remarks, true, true);
        }
        loanRepository.save(loan);
    }

    // --- SECRETARY ---
    @Transactional
    public void tableLoan(UUID loanId, LocalDateTime meetingDate) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.VERIFIED && loan.getStatus() != Loan.LoanStatus.APPROVED) {
            throw new RuntimeException("Loan must be verified by an officer before scheduling.");
        }

        loan.setStatus(Loan.LoanStatus.SECRETARY_TABLED);
        loan.setMeetingDate(meetingDate);
        loanRepository.save(loan);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' hh:mm a");
        String formattedDate = meetingDate.format(formatter);

        // Notify Applicant
        notificationService.notifyUser(loan.getMember().getId(), "Meeting Scheduled",
                "Your loan request will be reviewed by the committee on " + formattedDate + ". Please be ready.", true, true);

        // Notify Chairperson
        List<User> chairpersons = userRepository.findByRole(User.Role.CHAIRPERSON);
        for (User chair : chairpersons) {
            notificationService.notifyUser(chair.getId(), "Agenda Item Scheduled",
                    "Loan " + loan.getLoanNumber() + " tabled for voting on " + formattedDate + ".", true, false);
        }
    }

    // --- CHAIRPERSON STARTS VOTING ---
    @Transactional
    public void startVoting(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.SECRETARY_TABLED) {
            throw new RuntimeException("Loan is not scheduled for a meeting yet.");
        }

        // Time Check
        if (loan.getMeetingDate() != null && LocalDateTime.now().isBefore(loan.getMeetingDate())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM HH:mm");
            throw new RuntimeException("Cannot start voting yet. Meeting scheduled for " + loan.getMeetingDate().format(formatter));
        }

        // ✅ FIX: Initialize counters safely to prevent NULL errors later
        if (loan.getVotesYes() == null) loan.setVotesYes(0);
        if (loan.getVotesNo() == null) loan.setVotesNo(0);

        loan.setStatus(Loan.LoanStatus.VOTING_OPEN);
        loan.setVotingOpen(true);
        loanRepository.save(loan);

        log.info("Voting opened for loan {}", loan.getLoanNumber());

        // Notify Applicant (Status Update Only)
        notificationService.notifyUser(loan.getMember().getId(), "Voting Started",
                "The committee is now voting on your loan application.", false, true);

        // ✅ NOTE: We do NOT send "Please Vote" notifications to the applicant here.
        // The dashboard logic (LoanReadService) already filters out their own loan from the "Active Votes" list.
    }

    // --- COMMITTEE MEMBER ---
    @Transactional
    public void castVote(UUID loanId, UUID userId, boolean voteYes) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.VOTING_OPEN) {
            throw new RuntimeException("Voting is not currently open for this loan.");
        }

        // 1. Prevent Applicant from Voting
        if (loan.getMember().getUser().getId().equals(userId)) {
            throw new RuntimeException("Conflict of Interest: You cannot vote on your own loan.");
        }

        // 2. Initialize Lists/Counters (Fixes NPE)
        if (loan.getVotedUserIds() == null) loan.setVotedUserIds(new ArrayList<>());
        if (loan.getVotesYes() == null) loan.setVotesYes(0);
        if (loan.getVotesNo() == null) loan.setVotesNo(0);

        // 3. Prevent Double Voting
        if (loan.getVotedUserIds().contains(userId)) {
            throw new RuntimeException("You have already voted on this loan.");
        }

        // 4. Record Vote
        if (voteYes) {
            loan.setVotesYes(loan.getVotesYes() + 1);
        } else {
            loan.setVotesNo(loan.getVotesNo() + 1);
        }

        loan.getVotedUserIds().add(userId);
        loanRepository.save(loan);
    }

    // --- SECRETARY FINALIZES VOTE ---
    @Transactional
    public void closeVoting(UUID loanId, boolean approved, String minutes) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setVotingOpen(false);
        loan.setSecretaryComments(minutes);

        if (approved) {
            loan.setStatus(Loan.LoanStatus.SECRETARY_DECISION);
        } else {
            loan.setStatus(Loan.LoanStatus.REJECTED);
            loan.setRejectionReason(minutes);
        }
        loanRepository.save(loan);
    }

    // --- CHAIRPERSON FINAL RATIFICATION ---
    @Transactional
    public void chairpersonFinalApprove(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.SECRETARY_DECISION) {
            throw new RuntimeException("Loan must pass committee voting before final approval.");
        }

        loan.setStatus(Loan.LoanStatus.TREASURER_DISBURSEMENT);
        loanRepository.save(loan);
        notificationService.notifyUser(loan.getMember().getId(), "Final Approval", "Loan approved. Waiting for disbursement.", true, true);
    }
}