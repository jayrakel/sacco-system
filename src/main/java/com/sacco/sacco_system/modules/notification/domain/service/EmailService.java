package com.sacco.sacco_system.modules.notification.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.notification.domain.entity.NotificationLog;
import com.sacco.sacco_system.modules.notification.domain.repository.NotificationLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SystemSettingService systemSettingService;
    private final NotificationLogRepository notificationLogRepository; // ✅ Inject Repository

    @Value("${app.email.from:no-reply@sacco.com}")
    private String fromEmail;

    // --- 1. Specific Business Methods ---

    @Async
    public void sendVerificationEmail(String to, String firstName, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String content = String.format(
                "<p>Welcome to <strong>%s</strong>. Please verify your account.</p>" +
                        "<a href='%s'>Verify My Account</a>",
                getSaccoName(), verifyLink
        );
        // We log this as "MEMBER_VERIFICATION" type
        sendHtmlEmail(to, "Verify Your Account", "Welcome " + firstName, content, "MEMBER_VERIFICATION", "MEMBER", null);
    }

    @Async
    public void sendVerificationEmail(String to, String firstName, String tempPassword, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String content = String.format(
                "<p>Your administrator account has been created. <strong>%s</strong></p>" +
                        "<p>Temp Password: %s</p>" +
                        "<a href='%s'>Verify & Login</a>",
                getSaccoName(), tempPassword, verifyLink
        );
        sendHtmlEmail(to, "Admin Account Setup", "Welcome " + firstName, content, "ADMIN_VERIFICATION", "USER", null);
    }

    @Async
    public void resendVerificationToken(String to, String firstName, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String content = String.format(
                "<p>New verification link: <a href='%s'>Verify Now</a></p>",
                verifyLink
        );
        sendHtmlEmail(to, "Verify Email", "Hello " + firstName, content, "RESEND_VERIFICATION", "MEMBER", null);
    }

    @Async
    public void sendLoanStatusEmail(String to, String loanRef, String status) {
        String content = String.format(
                "<p>Your loan (Ref: <strong>%s</strong>) has been updated to <strong>%s</strong>.</p>",
                loanRef, status
        );
        // Log as LOAN_UPDATE related to a LOAN
        sendHtmlEmail(to, "Loan Status Update", "Loan #" + loanRef, content, "LOAN_STATUS_UPDATE", "LOAN", loanRef);
    }

    @Async
    public void sendMemberWelcomeEmail(String to, String name, String tempPassword, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String content = String.format(
                "<p>Welcome <strong>%s</strong>! Your temp password is <b>%s</b>.</p>" +
                        "<a href='%s'>Login Now</a>",
                name, tempPassword, verifyLink
        );
        sendHtmlEmail(to, "Welcome to " + getSaccoName(), "Welcome!", content, "MEMBER_WELCOME", "MEMBER", name);
    }

    @Async
    public void sendPasswordResetEmail(String to, String firstName, String token) {
        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        String content = String.format(
                "<p>Reset your password here: <a href='%s'>Reset Password</a></p>",
                resetLink
        );
        sendHtmlEmail(to, "Password Reset", "Hello " + firstName, content, "PASSWORD_RESET", "USER", null);
    }

    // --- 2. Generic Methods ---

    @Async
    public void sendGenericEmail(String to, String subject, String messageBody) {
        sendHtmlEmail(to, subject, "Notification", messageBody.replace("\n", "<br>"), "GENERIC_EMAIL", null, null);
    }

    // ✅ FIXED: This was the missing method causing your error
    @Async
    public void sendEmail(String to, String subject, String messageBody) {
        sendHtmlEmail(to, subject, subject, messageBody.replace("\n", "<br>"), "GENERIC_EMAIL", null, null);
    }

    // --- 3. Core Sender (With Logging) ---

    private void sendHtmlEmail(String to, String subject, String title, String bodyContent,
                               String templateCode, String refType, String refId) {

        NotificationLog.NotificationLogBuilder logBuilder = NotificationLog.builder()
                .templateCode(templateCode)
                .channel(NotificationLog.NotificationChannel.EMAIL)
                .recipient(to)
                .referenceType(refType)
                .referenceId(refId)
                .sentAt(LocalDateTime.now());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String saccoEmail = systemSettingService.getString("SACCO_EMAIL", fromEmail);
            String displayName = getSaccoName();

            helper.setFrom(displayName + " <" + saccoEmail + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildMasterTemplate(title, bodyContent), true);

            mailSender.send(message);

            // ✅ Log Success
            log.info("✅ Email sent to {}", to);
            logBuilder.status(NotificationLog.NotificationStatus.SENT);

        } catch (MessagingException e) {
            // ❌ Log Failure (Mail construction error)
            log.error("❌ Failed to construct email to {}", to, e);
            logBuilder.status(NotificationLog.NotificationStatus.FAILED);
            logBuilder.failureReason(e.getMessage());
        } catch (Exception e) {
            // ❌ Log Failure (Network/Server error)
            log.error("❌ Unexpected error sending email to {}", to, e);
            logBuilder.status(NotificationLog.NotificationStatus.FAILED);
            logBuilder.failureReason(e.getMessage());
        } finally {
            // ✅ Persist Log to Database (Section 48 Compliance)
            try {
                notificationLogRepository.save(logBuilder.build());
            } catch (Exception e) {
                log.error("Failed to save notification log", e);
            }
        }
    }

    private String buildMasterTemplate(String title, String content) {
        String saccoName = getSaccoName();
        String primaryColor = getPrimaryColor();
        String logoUrl = getLogoUrl();

        return String.format(
                """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#f3f4f6; font-family:sans-serif;">
                    <table width="100%%" style="padding: 40px;">
                        <tr>
                            <td align="center">
                                <div style="background:#fff; border-radius:8px; overflow:hidden; max-width:600px; box-shadow:0 4px 6px rgba(0,0,0,0.1);">
                                    <div style="background:%s; padding:20px; text-align:center;">
                                        %s
                                        <h2 style="color:#fff; margin:10px 0;">%s</h2>
                                    </div>
                                    <div style="padding:30px; color:#333; line-height:1.6;">
                                        <h3 style="margin-top:0;">%s</h3>
                                        %s
                                    </div>
                                    <div style="background:#f9fafb; padding:20px; text-align:center; font-size:12px; color:#666;">
                                        &copy; %s %s
                                    </div>
                                </div>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                primaryColor,
                (logoUrl != null ? "<img src='" + logoUrl + "' height='40' style='background:#fff; padding:5px; border-radius:4px;'><br>" : ""),
                saccoName,
                title,
                content,
                java.time.Year.now().getValue(), saccoName
        );
    }

    private String getSaccoName() { return systemSettingService.getString("SACCO_NAME", "Sacco System"); }
    private String getPrimaryColor() { return systemSettingService.getString("BRAND_COLOR_PRIMARY", "#059669"); }
    private String getLogoUrl() { return systemSettingService.getString("SACCO_LOGO"); }
}