package com.sacco.sacco_system.service;

import com.sacco.sacco_system.dto.MemberDTO;
import com.sacco.sacco_system.entity.Member;
import com.sacco.sacco_system.repository.MemberRepository;
import com.sacco.sacco_system.repository.SavingsAccountRepository;
import com.sacco.sacco_system.repository.ShareCapitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {
    
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final ShareCapitalRepository shareCapitalRepository;
    
    public MemberDTO createMember(MemberDTO memberDTO) {
        Member member = Member.builder()
                .firstName(memberDTO.getFirstName())
                .lastName(memberDTO.getLastName())
                .email(memberDTO.getEmail())
                .phoneNumber(memberDTO.getPhoneNumber())
                .idNumber(memberDTO.getIdNumber())
                .address(memberDTO.getAddress())
                .dateOfBirth(memberDTO.getDateOfBirth())
                .status(Member.MemberStatus.ACTIVE)
                .memberNumber(generateMemberNumber())
                .totalShares(BigDecimal.ZERO)
                .totalSavings(BigDecimal.ZERO)
                .build();
        
        Member savedMember = memberRepository.save(member);
        return convertToDTO(savedMember);
    }
    
    public MemberDTO getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));
        return convertToDTO(member);
    }
    
    public MemberDTO getMemberByMemberNumber(String memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new RuntimeException("Member not found with number: " + memberNumber));
        return convertToDTO(member);
    }
    
    public List<MemberDTO> getAllMembers() {
        return memberRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<MemberDTO> getActiveMembers() {
        return memberRepository.findByStatus(Member.MemberStatus.ACTIVE)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public MemberDTO updateMember(Long id, MemberDTO memberDTO) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));
        
        member.setFirstName(memberDTO.getFirstName());
        member.setLastName(memberDTO.getLastName());
        member.setEmail(memberDTO.getEmail());
        member.setPhoneNumber(memberDTO.getPhoneNumber());
        member.setAddress(memberDTO.getAddress());
        member.setDateOfBirth(memberDTO.getDateOfBirth());
        
        Member updatedMember = memberRepository.save(member);
        return convertToDTO(updatedMember);
    }
    
    public void deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));
        
        member.setStatus(Member.MemberStatus.INACTIVE);
        memberRepository.save(member);
    }
    
    public long getActiveMembersCount() {
        return memberRepository.countActiveMembers();
    }
    
    private String generateMemberNumber() {
        long count = memberRepository.count();
        return "MEM" + String.format("%06d", count + 1);
    }
    
    private MemberDTO convertToDTO(Member member) {
        return MemberDTO.builder()
                .id(member.getId())
                .memberNumber(member.getMemberNumber())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .idNumber(member.getIdNumber())
                .address(member.getAddress())
                .dateOfBirth(member.getDateOfBirth())
                .status(member.getStatus().toString())
                .totalShares(member.getTotalShares())
                .totalSavings(member.getTotalSavings())
                .build();
    }
}
