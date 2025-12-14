package com.sacco.sacco_system.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Async // Run in background so the UI doesn't freeze
    public void sendVerificationEmail(String to, String firstName, String tempPassword, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Action Required: Verify Your Sacco Account");

            String verifyLink = "http://localhost:5173/verify-email?token=" + token;

            String htmlContent = String.format(
                    """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                        <h2 style="color: #059669;">Welcome to Sacco System</h2>
                        <p>Hello <strong>%s</strong>,</p>
                        <p>An account has been created for you as an administrator.</p>
                        
                        <div style="background-color: #f9fafb; padding: 15px; border-radius: 5px; margin: 20px 0;">
                            <p style="margin: 0;"><strong>Temporary Password:</strong> <span style="font-family: monospace; font-size: 16px; color: #d97706;">%s</span></p>
                        </div>
    
                        <p>Please verify your email address to activate your account and set a new password.</p>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background-color: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">Verify Account</a>
                        </div>
                        
                        <p style="font-size: 12px; color: #6b7280;">If the button above doesn't work, copy and paste this link:<br>%s</p>
                    </div>
                    """,
                    firstName, tempPassword, verifyLink, verifyLink
            );

            helper.setText(htmlContent, true); // true = HTML
            mailSender.send(message);
            System.out.println("✅ Email sent successfully to " + to);

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            throw new RuntimeException("Failed to send email");
        }
    }

    // ✅ ADD THIS NEW METHOD HERE 👇
    @Async
    public void resendVerificationToken(String to, String firstName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Verify Your Email Address");

            String verifyLink = "http://localhost:5173/verify-email?token=" + token;

            String htmlContent = String.format(
                    """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                        <h2 style="color: #059669;">Verify Your Email</h2>
                        <p>Hello <strong>%s</strong>,</p>
                        <p>You requested a new verification link for your Sacco System account.</p>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background-color: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">Verify Now</a>
                        </div>
                        
                        <p style="font-size: 12px; color: #6b7280;">Link expires in 24 hours.<br>If you did not request this, please ignore this email.</p>
                    </div>
                    """,
                    firstName, verifyLink
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("✅ Resend email sent to " + to);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email");
        }
    }
}