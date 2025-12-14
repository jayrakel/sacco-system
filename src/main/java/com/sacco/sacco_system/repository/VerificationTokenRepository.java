package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.VerificationToken;
import com.sacco.sacco_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // <--- Import this

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);

    @Transactional // <--- ADD THIS ANNOTATION
    void deleteByUser(User user);
}