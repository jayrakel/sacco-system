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

    // --- Core Lookups ---

    /**
     * Linkage: Finds the Member profile linked to an Auth User.
     */
    Optional<Member> findByUserId(UUID userId);

    /**
     * Business Key Lookup (Human-readable)
     */
    Optional<Member> findByMemberNumber(String memberNumber);

    /**
     * Business Key Lookup (System-generated)
     * Added per Dictionary Section 3
     */
    Optional<Member> findByMemberId(String memberId);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByPhoneNumber(String phoneNumber);

    // Renamed from findByIdNumber -> findByNationalId to match Dictionary Section 3
    Optional<Member> findByNationalId(String nationalId);

    // --- Status Lookups ---

    // Renamed from findByStatus -> findByMemberStatus to match Entity field 'memberStatus'
    List<Member> findByMemberStatus(Member.MemberStatus memberStatus);

    // JPQL updated to use 'memberStatus' instead of 'status'
    @Query("SELECT COUNT(m) FROM Member m WHERE m.memberStatus = 'ACTIVE'")
    long countActiveMembers();
}