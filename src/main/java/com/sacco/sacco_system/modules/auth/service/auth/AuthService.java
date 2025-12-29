package com.sacco.sacco_system.modules.auth.service.auth;

import com.sacco.sacco_system.modules.auth.dto.AuthRequest;
import com.sacco.sacco_system.modules.auth.dto.AuthResponse;
import com.sacco.sacco_system.modules.auth.dto.ChangePasswordRequest;
import com.sacco.sacco_system.modules.auth.service.auth.impl.AuthLoginHandler;
import com.sacco.sacco_system.modules.auth.service.auth.impl.AuthPasswordHandler;
import com.sacco.sacco_system.modules.auth.service.auth.impl.AuthRegistrationHandler;
import com.sacco.sacco_system.modules.auth.service.auth.impl.AuthVerificationHandler;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthRegistrationHandler registrationHandler;
    private final AuthLoginHandler loginHandler;
    private final AuthPasswordHandler passwordHandler;
    private final AuthVerificationHandler verificationHandler;

    // --- REGISTRATION ---

    public Map<String, Object> register(User user) {
        return registrationHandler.register(user);
    }

    // --- LOGIN / LOGOUT ---

    public AuthResponse login(AuthRequest request) {
        return loginHandler.login(request);
    }

    public void logout(User user) {
        loginHandler.logout(user);
    }

    // --- PASSWORD MANAGEMENT ---

    public void changePassword(ChangePasswordRequest request) {
        passwordHandler.changePassword(request);
    }

    public void forgotPassword(String email) {
        passwordHandler.forgotPassword(email);
    }

    public void resetPassword(String token, String newPassword) {
        passwordHandler.resetPassword(token, newPassword);
    }

    // --- VERIFICATION ---

    public void verifyEmail(String token) {
        verificationHandler.verifyEmail(token);
    }

    public void resendVerificationEmail(String email) {
        verificationHandler.resendVerificationEmail(email);
    }
}