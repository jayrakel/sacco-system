package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanGovernanceService {

    private final LoanRepository loanRepository;
    private final NotificationService notificationService;

    // --- LOAN OFFICER ---
    @Transactional
    public void reviewApplication(UUID loanId, String decision, String remarks) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();

        if ("APPROVE".equalsIgnoreCase(decision)) {
            loan.setStatus(Loan.LoanStatus.APPROVED);
            loan.setApprovalDate(LocalDate.now());
            notificationService.notifyUser(loan.getMember().getId(), "Approved", "Loan approved by Officer.", true, true);
        } else {
            loan.setStatus(Loan.LoanStatus.REJECTED);
            loan.setRejectionReason(remarks);
        }
        loanRepository.save(loan);
    }

    // --- SECRETARY ---
    @Transactional
    public void tableLoan(UUID loanId, LocalDateTime meetingDate) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setStatus(Loan.LoanStatus.SECRETARY_TABLED);
        loan.setMeetingDate(meetingDate);
        loanRepository.save(loan);
    }

    @Transactional
    public void startVoting(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setStatus(Loan.LoanStatus.VOTING_OPEN);
        loan.setVotingOpen(true);
        loanRepository.save(loan);
    }

    @Transactional
    public void closeVoting(UUID loanId, boolean approved, String minutes) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setVotingOpen(false);
        loan.setSecretaryComments(minutes);

        if (approved) {
            loan.setStatus(Loan.LoanStatus.SECRETARY_DECISION);
        } else {
            loan.setStatus(Loan.LoanStatus.REJECTED);
        }
        loanRepository.save(loan);
    }

    // --- COMMITTEE MEMBER ---
    @Transactional
    public void castVote(UUID loanId, UUID userId, boolean voteYes) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        // ... (Insert checks for conflicts/double voting from original file) ...

        if (voteYes) loan.setVotesYes(loan.getVotesYes() + 1);
        else loan.setVotesNo(loan.getVotesNo() + 1);

        if (loan.getVotedUserIds() == null) loan.setVotedUserIds(new ArrayList<>());
        loan.getVotedUserIds().add(userId);

        loanRepository.save(loan);
    }

    // --- CHAIRPERSON ---
    @Transactional
    public void chairpersonFinalApprove(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setStatus(Loan.LoanStatus.TREASURER_DISBURSEMENT);
        loanRepository.save(loan);
        notificationService.notifyUser(loan.getMember().getId(), "Final Approval", "Loan ready for disbursement.", true, true);
    }
}