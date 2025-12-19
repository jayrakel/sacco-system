package com.sacco.sacco_system.modules.auth.repository; // ✅ New Package

import com.sacco.sacco_system.modules.auth.model.User; // ✅ Import new User location
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByRole(User.Role role);
}