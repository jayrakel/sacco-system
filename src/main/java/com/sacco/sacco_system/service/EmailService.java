package com.sacco.sacco_system.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ✅ Logging
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    // --- 1. SYSTEM ADMIN SETUP EMAIL ---
    @Async
    public void sendVerificationEmail(String to, String firstName, String tempPassword, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String subject = "Action Required: Verify Your Sacco Account";

        String htmlContent = String.format(
                """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                    <h2 style="color: #059669;">Welcome to Sacco System</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>An account has been created for you as an administrator.</p>
                    
                    <div style="background-color: #f9fafb; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>Temporary Password:</strong> <span style="font-family: monospace; font-size: 16px; color: #d97706;">%s</span></p>
                    </div>
    
                    <p>Please verify your email address to activate your account.</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">Verify Account</a>
                    </div>
                </div>
                """,
                firstName, tempPassword, verifyLink
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    // --- 2. RESEND VERIFICATION LINK ---
    @Async
    public void resendVerificationToken(String to, String firstName, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String subject = "Verify Your Email Address";

        String htmlContent = String.format(
                """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                    <h2 style="color: #059669;">Verify Your Email</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>You requested a new verification link.</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">Verify Now</a>
                    </div>
                    
                    <p style="font-size: 12px; color: #6b7280;">Link expires in 24 hours.</p>
                </div>
                """,
                firstName, verifyLink
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    // --- 3. NEW MEMBER WELCOME EMAIL ---
    @Async
    public void sendMemberWelcomeEmail(String to, String name, String tempPassword, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String subject = "Welcome to Sacco System - Login Details";

        String htmlContent = String.format(
                """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                    <h2 style="color: #059669;">Welcome, %s!</h2>
                    <p>Your member registration is complete.</p>
                    
                    <div style="background-color: #f0fdf4; padding: 15px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #059669;">
                        <p style="margin: 5px 0;"><strong>Username:</strong> %s</p>
                        <p style="margin: 5px 0;"><strong>Temporary Password:</strong> <span style="font-family: monospace; font-size: 16px; background: #fff; padding: 2px 6px; border-radius: 4px; border: 1px solid #ddd;">%s</span></p>
                    </div>
    
                    <p><strong>Action Required:</strong></p>
                    <ol>
                        <li>Click the button below to verify your email.</li>
                        <li>Log in using the temporary password.</li>
                    </ol>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">Verify & Login</a>
                    </div>
                </div>
                """,
                name, to, tempPassword, verifyLink
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    // --- HELPER: GENERIC SENDER ---
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email");
        }
    }
}