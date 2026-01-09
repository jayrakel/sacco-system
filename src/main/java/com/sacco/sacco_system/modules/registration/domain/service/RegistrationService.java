package com.sacco.sacco_system.modules.registration.domain.service;

import com.sacco.sacco_system.modules.member.api.dto.MemberDTO;
import com.sacco.sacco_system.modules.member.domain.service.MemberService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.service.UserService;
import com.sacco.sacco_system.modules.auth.repository.VerificationTokenRepository;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Coordinates the registration of new members.
 * Orchestrates User creation (authentication) and Member creation (SACCO membership).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserService userService;
    private final MemberService memberService;
    private final EmailService emailService;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberDTO registerMember(MemberDTO memberDTO, MultipartFile file, String paymentMethod, String referenceCode, String bankAccountCode) throws IOException {
        log.info("Starting member registration for email: {}", memberDTO.getEmail());

        // Step 1: Create User account (authentication)
        User user = userService.createUser(
                memberDTO.getFirstName(),
                memberDTO.getLastName(),
                memberDTO.getEmail(),
                memberDTO.getPhoneNumber(),
                User.Role.MEMBER
        );

        // Step 2: Generate the ACTUAL temporary password for email
        String tempPassword = userService.generateTemporaryPassword();

        // ✅ Update the user's password in the DB to match the one we are sending
        user.setPasswordHash(passwordEncoder.encode(tempPassword));

        // Step 3: Create Member record (SACCO membership)
        // ✅ FIX: Passed bankAccountCode correctly instead of null
        MemberDTO createdMember = memberService.createMember(memberDTO, file, paymentMethod, referenceCode, bankAccountCode, user);

        // Step 4: Create email verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(user, token);
        tokenRepository.save(verificationToken);

        // Step 5: Send welcome email with the CORRECT password
        try {
            emailService.sendMemberWelcomeEmail(user.getEmail(), user.getFirstName(), tempPassword, token);
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage());
        }

        log.info("Member registration completed successfully for: {}", memberDTO.getEmail());
        return createdMember;
    }
}