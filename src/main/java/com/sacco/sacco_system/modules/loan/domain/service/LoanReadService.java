package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanReadService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final GuarantorRepository guarantorRepository;

    public LoanDTO getLoanById(UUID loanId) {
        return loanRepository.findById(loanId).map(LoanDTO::fromEntity).orElseThrow();
    }

    public List<LoanDTO> getMemberLoans(UUID userId) {
        return memberRepository.findByUserId(userId)
                .map(member -> loanRepository.findByMemberId(member.getId()).stream()
                        .map(LoanDTO::fromEntity)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public List<LoanDTO> getPendingLoansForAdmin() {
        return loanRepository.findByStatusIn(List.of(
                Loan.LoanStatus.SUBMITTED, Loan.LoanStatus.LOAN_OFFICER_REVIEW, Loan.LoanStatus.APPROVED,
                Loan.LoanStatus.SECRETARY_TABLED, Loan.LoanStatus.VOTING_OPEN,
                Loan.LoanStatus.SECRETARY_DECISION, Loan.LoanStatus.TREASURER_DISBURSEMENT
        )).stream().map(LoanDTO::fromEntity).collect(Collectors.toList());
    }

    public List<GuarantorDTO> getGuarantorRequests(UUID userId) {
        return memberRepository.findByUserId(userId)
                .map(member -> guarantorRepository.findByMemberAndStatus(member, Guarantor.GuarantorStatus.PENDING)
                        .stream().map(GuarantorDTO::fromEntity).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public List<GuarantorDTO> getGuarantorsByLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        return loan.getGuarantors().stream()
                .map(GuarantorDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<LoanDTO> getActiveVotesForMember(UUID userId) {
        List<Loan> openLoans = loanRepository.findByStatus(Loan.LoanStatus.VOTING_OPEN);
        return openLoans.stream()
                .filter(l -> !l.getVotedUserIds().contains(userId))
                .map(LoanDTO::fromEntity)
                .collect(Collectors.toList());
    }
}