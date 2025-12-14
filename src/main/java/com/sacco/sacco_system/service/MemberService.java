package com.sacco.sacco_system.service;

import com.sacco.sacco_system.dto.MemberDTO;
import com.sacco.sacco_system.entity.Member;
import com.sacco.sacco_system.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberDTO createMember(MemberDTO memberDTO) {
        Member member = Member.builder()
                .firstName(memberDTO.getFirstName())
                .lastName(memberDTO.getLastName())
                .email(memberDTO.getEmail())
                .phoneNumber(memberDTO.getPhoneNumber())
                .idNumber(memberDTO.getIdNumber())
                .kraPin(memberDTO.getKraPin()) // New
                .nextOfKinName(memberDTO.getNextOfKinName()) // New
                .nextOfKinPhone(memberDTO.getNextOfKinPhone()) // New
                .nextOfKinRelation(memberDTO.getNextOfKinRelation()) // New
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

    public MemberDTO updateMember(UUID id, MemberDTO memberDTO) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));

        member.setFirstName(memberDTO.getFirstName());
        member.setLastName(memberDTO.getLastName());
        member.setEmail(memberDTO.getEmail());
        member.setPhoneNumber(memberDTO.getPhoneNumber());
        member.setAddress(memberDTO.getAddress());
        member.setDateOfBirth(memberDTO.getDateOfBirth());

        // Update new fields
        member.setKraPin(memberDTO.getKraPin());
        member.setNextOfKinName(memberDTO.getNextOfKinName());
        member.setNextOfKinPhone(memberDTO.getNextOfKinPhone());
        member.setNextOfKinRelation(memberDTO.getNextOfKinRelation());

        Member updatedMember = memberRepository.save(member);
        return convertToDTO(updatedMember);
    }

    // ... (Keep existing getters, delete, count methods exactly as they were) ...
    public MemberDTO getMemberById(UUID id) {
        return memberRepository.findById(id).map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public MemberDTO getMemberByMemberNumber(String memberNumber) {
        return memberRepository.findByMemberNumber(memberNumber).map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public List<MemberDTO> getAllMembers() {
        return memberRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MemberDTO> getActiveMembers() {
        return memberRepository.findByStatus(Member.MemberStatus.ACTIVE).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public void deleteMember(UUID id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Member not found"));
        member.setStatus(Member.MemberStatus.INACTIVE);
        memberRepository.save(member);
    }

    public long getActiveMembersCount() { return memberRepository.countActiveMembers(); }

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
                .kraPin(member.getKraPin()) // New
                .nextOfKinName(member.getNextOfKinName()) // New
                .nextOfKinPhone(member.getNextOfKinPhone()) // New
                .nextOfKinRelation(member.getNextOfKinRelation()) // New
                .address(member.getAddress())
                .dateOfBirth(member.getDateOfBirth())
                .status(member.getStatus().toString())
                .totalShares(member.getTotalShares())
                .totalSavings(member.getTotalSavings())
                .build();
    }
}