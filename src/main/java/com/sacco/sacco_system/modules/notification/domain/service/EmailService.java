package com.sacco.sacco_system.modules.notification.domain.service;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // --- 1. SYSTEM ADMIN SETUP EMAIL (4 Arguments) ---
    @Async
    public void sendVerificationEmail(String to, String firstName, String tempPassword, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String subject = "Action Required: Verify Your Sacco Account";

        String htmlContent = String.format(
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; background-color: #f3f4f6; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f3f4f6; padding: 40px 20px;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                                    <!-- Header with gradient -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #059669 0%%, #047857 100%%; padding: 40px 30px; text-align: center;">
                                            <h1 style="margin: 0; color: #030120ff; font-size: 28px; font-weight: 600;">Secure Sacco</h1>
                                            <p style="margin: 10px 0 0 0; color: #03351bff; font-size: 14px;">Member Management System</p>
                                        </td>
                                    </tr>
                                    
                                    <!-- Content -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <h2 style="margin: 0 0 20px 0; color: #111827; font-size: 24px; font-weight: 600;">Welcome Aboard, %s!</h2>
                                            <p style="margin: 0 0 20px 0; color: #4b5563; font-size: 16px; line-height: 1.6;">
                                                Your administrator account has been successfully created. You're now part of the Secure Sacco management team.
                                            </p>
                                            
                                            <!-- Credentials Box -->
                                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0; background: linear-gradient(135deg, #fef3c7 0%%, #fde68a 100%%); border-radius: 8px; border-left: 4px solid #f59e0b;">
                                                <tr>
                                                    <td style="padding: 20px;">
                                                        <p style="margin: 0 0 10px 0; color: #78350f; font-size: 14px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px;">Your Login Credentials</p>
                                                        <div style="background-color: #ffffff; padding: 15px; border-radius: 6px; margin-top: 10px;">
                                                            <p style="margin: 0; color: #1f2937; font-size: 14px;">
                                                                <strong>Temporary Password:</strong>
                                                            </p>
                                                            <p style="margin: 8px 0 0 0; font-family: 'Courier New', monospace; font-size: 18px; color: #dc2626; font-weight: bold; letter-spacing: 1px;">
                                                                %s
                                                            </p>
                                                        </div>
                                                        <p style="margin: 15px 0 0 0; color: #92400e; font-size: 13px; line-height: 1.5;">
                                                            <strong>⚠️ Important:</strong> You'll be required to change this password after your first login for security purposes.
                                                        </p>
                                                        <p style="margin: 10px 0 0 0; color: #92400e; font-size: 13px; line-height: 1.5;">
                                                            <strong>⏱️ Note:</strong> This verification link and password will expire in <strong>24 hours</strong>. Please verify your account promptly.
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                            
                                            <!-- Action Steps -->
                                            <div style="margin: 30px 0;">
                                                <p style="margin: 0 0 15px 0; color: #1f2937; font-size: 16px; font-weight: 600;">Next Steps:</p>
                                                <ol style="margin: 0; padding-left: 20px; color: #4b5563; font-size: 15px; line-height: 1.8;">
                                                    <li>Click the button below to verify your email address</li>
                                                    <li>Login using your credentials</li>
                                                    <li>Create a new secure password</li>
                                                    <li>Complete your profile setup</li>
                                                </ol>
                                            </div>
                                            
                                            <!-- CTA Button -->
                                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #059669 0%%, #047857 100%%); color: #ffffff; padding: 16px 40px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px; box-shadow: 0 4px 6px rgba(5, 150, 105, 0.3);">
                                                            Verify Email & Activate Account
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                                            
                                            <!-- Security Note -->
                                            <div style="margin-top: 30px; padding: 15px; background-color: #f9fafb; border-left: 3px solid #6b7280; border-radius: 4px;">
                                                <p style="margin: 0; color: #4b5563; font-size: 13px; line-height: 1.6;">
                                                    <strong>🔒 Security Reminder:</strong> Never share your password with anyone. Our team will never ask for your password via email or phone.
                                                </p>
                                            </div>
                                        </td>
                                    </tr>
                                    
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #e5e7eb;">
                                            <p style="margin: 0 0 10px 0; color: #6b7280; font-size: 14px;">
                                                Need help? Contact our support team
                                            </p>
                                            <p style="margin: 0; color: #9ca3af; font-size: 12px;">
                                                © 2025 Secure Sacco. All rights reserved.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                firstName, tempPassword, verifyLink
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    // âœ… NEW: STANDARD REGISTRATION EMAIL (3 Arguments - Overloaded)
    // This fixes the error in AuthService.java
    @Async
    public void sendVerificationEmail(String to, String firstName, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String subject = "Verify Your Sacco Account";

        String htmlContent = String.format(
                """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                    <h2 style="color: #059669;">Welcome to Sacco System</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Thank you for registering. Please verify your email address to activate your account.</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">Verify Account</a>
                    </div>
                </div>
                """,
                firstName, verifyLink
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

    @Async
    public void sendPasswordResetEmail(String to, String firstName, String token) {
        // Point to your frontend route
        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        String subject = "Secure Password Reset Request";

        String htmlContent = String.format(
                """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.1); border: 1px solid #e5e7eb;">
                    <div style="background-color: #111827; padding: 20px; text-align: center;">
                        <h2 style="color: #ffffff; margin: 0; font-size: 20px;">Secure Sacco</h2>
                    </div>
                    <div style="padding: 40px 30px;">
                        <h2 style="color: #111827; font-size: 24px; margin-top: 0;">Password Reset Request</h2>
                        <p style="color: #4b5563; font-size: 16px; line-height: 1.6;">Hello <strong>%s</strong>,</p>
                        <p style="color: #4b5563; font-size: 16px; line-height: 1.6;">We received a request to reset the password for your account. If you didn't make this request, you can safely ignore this email.</p>
                        
                        <div style="text-align: center; margin: 35px 0;">
                            <a href="%s" style="background-color: #059669; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 16px; box-shadow: 0 2px 4px rgba(5, 150, 105, 0.2);">Reset My Password</a>
                        </div>
                        
                        <p style="color: #6b7280; font-size: 14px; text-align: center;">This link will expire in <strong>24 hours</strong>.</p>
                    </div>
                    <div style="background-color: #f9fafb; padding: 20px; text-align: center; border-top: 1px solid #e5e7eb;">
                        <p style="margin: 0; color: #9ca3af; font-size: 12px;">© 2025 Secure Sacco System</p>
                    </div>
                </div>
                """,
                firstName, resetLink
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    // --- HELPER: GENERIC SENDER ---
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            log.info("Attempting to send email to: {}", to);
            log.info("SMTP Config - Host: smtp.gmail.com, Port: 587, From: System Admin @ Secure Sacco <{}>", fromEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set sender with display name: "System Admin @ Secure Sacco <email@example.com>"
            helper.setFrom("System Admin @ Secure Sacco <" + fromEmail + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("SUCCESS: Email sent to {}", to);
        } catch (MessagingException e) {
            log.error("MESSAGING EXCEPTION - Failed to send email to: {}", to);
            log.error("Full error stack trace:", e);
            // Don't throw exception - just log it so transaction doesn't roll back
        } catch (Exception e) {
            log.error("UNEXPECTED ERROR sending email to: {}", to);
            log.error("Full error stack trace:", e);
            // Don't throw exception - just log it so transaction doesn't roll back
        }
    }
}

