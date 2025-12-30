package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanReadService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final GuarantorRepository guarantorRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    public LoanDTO getLoanById(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        return enrichLoanDTO(LoanDTO.fromEntity(loan), loan);
    }

    public List<LoanDTO> getMemberLoans(UUID userId) {
        return memberRepository.findByUserId(userId)
                .map(member -> loanRepository.findByMemberId(member.getId()).stream()
                        .map(loan -> enrichLoanDTO(LoanDTO.fromEntity(loan), loan))
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    /**
     * ✅ ADDED: This is the method causing your error.
     * It fetches loans waiting for the Treasurer.
     */
    public List<LoanDTO> getLoansReadyForDisbursement() {
        return loanRepository.findByStatus(Loan.LoanStatus.TREASURER_DISBURSEMENT)
                .stream()
                .map(loan -> enrichLoanDTO(LoanDTO.fromEntity(loan), loan))
                .collect(Collectors.toList());
    }

    public List<LoanDTO> getPendingLoansForAdmin() {
        return loanRepository.findByStatusIn(List.of(
                Loan.LoanStatus.SUBMITTED,
                Loan.LoanStatus.LOAN_OFFICER_REVIEW,
                Loan.LoanStatus.VERIFIED,
                Loan.LoanStatus.APPROVED,
                Loan.LoanStatus.SECRETARY_TABLED,
                Loan.LoanStatus.VOTING_OPEN,
                Loan.LoanStatus.SECRETARY_DECISION,
                Loan.LoanStatus.TREASURER_DISBURSEMENT
        )).stream().map(loan -> enrichLoanDTO(LoanDTO.fromEntity(loan), loan)).collect(Collectors.toList());
    }

    public List<GuarantorDTO> getGuarantorRequests(UUID userId) {
        return memberRepository.findByUserId(userId)
                .map(member -> guarantorRepository.findByMemberAndStatus(member, Guarantor.GuarantorStatus.PENDING)
                        .stream().map(GuarantorDTO::fromEntity).collect(Collectors.toList()))
                .orElse(List.of());
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
                // 1. Exclude loans already voted on
                .filter(l -> l.getVotedUserIds() == null || !l.getVotedUserIds().contains(userId))

                // 2. Exclude applicant's own loans
                .filter(l -> l.getMember() != null && !l.getMember().getUser().getId().equals(userId))

                .map(loan -> enrichLoanDTO(LoanDTO.fromEntity(loan), loan))
                .collect(Collectors.toList());
    }

    // --- HELPER ---
    private LoanDTO enrichLoanDTO(LoanDTO dto, Loan loan) {
        if (dto != null && loan.getMember() != null) {
            BigDecimal totalSavings = savingsAccountRepository.findByMember_Id(loan.getMember().getId()).stream()
                    .map(SavingsAccount::getBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setMemberSavings(totalSavings);
        }
        return dto;
    }
}