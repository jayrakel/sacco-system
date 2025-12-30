package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.loan.api.dto.LoanResponseDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanReadService {
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;

    public List<LoanResponseDTO> getMemberLoans(UUID userId) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        return loanRepository.findByMemberId(member.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private LoanResponseDTO mapToDTO(Loan loan) {
        return LoanResponseDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .productName(loan.getProduct().getName())
                .principalAmount(loan.getPrincipalAmount())
                .status(loan.getStatus().name())
                .applicationDate(loan.getApplicationDate())
                .build();
    }
}