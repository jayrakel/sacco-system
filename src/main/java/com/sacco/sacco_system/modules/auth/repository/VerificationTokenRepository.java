package com.sacco.sacco_system.modules.auth.repository; // âœ… New Package

import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    VerificationToken findByToken(String token);
    void deleteByUser(User user);
}

