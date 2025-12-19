package com.sacco.sacco_system.modules.members.repository;

import com.sacco.sacco_system.modules.members.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByMemberNumber(String memberNumber);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByPhoneNumber(String phoneNumber);
    Optional<Member> findByIdNumber(String idNumber);
    List<Member> findByStatus(Member.MemberStatus status);
    
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE'")
    long countActiveMembers();
}
