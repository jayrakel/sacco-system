package com.sacco.sacco_system.modules.core.config.seeder;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class AdminUserSeeder {

    private final UserService userService;

    @Value("${app.default-admin.email}")
    private String adminEmail;

    @Value("${app.default-admin.password}")
    private String adminPassword;

    @Value("${app.default-admin.phone}")
    private String adminPhone;

    @Value("${app.official-email-domain}")
    private String officialEmailDomain;

    @Transactional
    public void seed() {
        try {
            userService.getUserByEmail(adminEmail);
            // Admin exists, do nothing
        } catch (Exception e) {
            log.info("Creating default admin user: {}", adminEmail);

            String officialEmail = generateOfficialEmail(User.Role.ADMIN);

            userService.createBootstrapUser(
                    "System",
                    "Administrator",
                    adminEmail,
                    officialEmail,
                    adminPhone,
                    adminPassword,
                    User.Role.ADMIN
            );
        }
    }

    private String generateOfficialEmail(User.Role role) {
        return role.toString().toLowerCase().replace("_", "") + "@" + officialEmailDomain;
    }
}