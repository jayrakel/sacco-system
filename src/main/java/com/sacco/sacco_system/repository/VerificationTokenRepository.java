package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.VerificationToken;
import com.sacco.sacco_system.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);
    @Transactional
    void deleteByUser(User user);
}