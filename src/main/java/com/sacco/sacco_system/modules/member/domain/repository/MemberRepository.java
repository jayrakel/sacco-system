package com.sacco.sacco_system.modules.member.domain.repository;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {

    /**
     * Finds the Member record linked to a specific User UUID.
     * Essential for the Member Dashboard and Eligibility checks.
     */
    Optional<Member> findByUserId(UUID userId);

    Optional<Member> findByMemberNumber(String memberNumber);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByPhoneNumber(String phoneNumber);

    Optional<Member> findByNationalId(String nationalId);

    // ✅ Changed from findByStatus to findByMemberStatus to match Member.memberStatus field
    List<Member> findByMemberStatus(Member.MemberStatus memberStatus);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.memberStatus = 'ACTIVE'")
    long countActiveMembers();
}