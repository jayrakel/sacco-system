package com.sacco.sacco_system.modules.auth.repository; // âœ… New Package

import com.sacco.sacco_system.modules.auth.model.User; // âœ… Import new User location
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByOfficialEmail(String officialEmail);

    // Find by either personal or official email
    default Optional<User> findByEmailOrOfficialEmail(String email) {
        Optional<User> user = findByEmail(email);
        if (user.isEmpty()) {
            user = findByOfficialEmail(email);
        }
        return user;
    }

    boolean existsByRole(User.Role role);

    List<User> findByRole(User.Role role);
}

