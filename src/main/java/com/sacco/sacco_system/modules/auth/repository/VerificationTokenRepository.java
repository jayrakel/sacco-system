package com.sacco.sacco_system.modules.auth.repository; // âœ… New Package

import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    VerificationToken findByToken(String token);
    
    @Modifying
    @Transactional
    void deleteByUser(User user);
}

