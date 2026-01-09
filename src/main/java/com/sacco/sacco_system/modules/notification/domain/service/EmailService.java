package com.sacco.sacco_system.modules.notification.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
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
    private final SystemSettingService systemSettingService;

    @Value("${app.email.from:no-reply@sacco.com}")
    private String fromEmail;

    // ✅ FIX: Overloaded Method 1 (4 Arguments) - Matches AuthService call
    @Async
    public void sendVerificationEmail(String to, String firstName, String tempPassword, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String content = String.format(
                "<p>Your administrator account has been successfully created. You're now part of the <strong>%s</strong> management team.</p>" +
                        "<div style='background: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0;'>" +
                        "  <p style='margin: 0; font-size: 14px; font-weight: bold; color: #92400e;'>Your Temporary Credentials</p>" +
                        "  <p style='margin: 10px 0 0 0; font-family: monospace; font-size: 18px; color: #dc2626;'>%s</p>" +
                        "</div>" +
                        "<p>Please verify your email and change this password immediately upon login.</p>" +
                        "<div style='text-align: center; margin: 30px 0;'>" +
                        "  <a href='%s' style='background-color: %s; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Verify & Login</a>" +
                        "</div>",
                getSaccoName(), tempPassword, verifyLink, getPrimaryColor()
        );

        sendHtmlEmail(to, "Action Required: Admin Account Setup", "Welcome Aboard, " + firstName + "!", content);
    }

    // ✅ FIX: Overloaded Method 2 (3 Arguments) - Standard Registration
    @Async
    public void sendVerificationEmail(String to, String firstName, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String content = String.format(
                "<p>Thank you for registering with <strong>%s</strong>. To ensure the security of your account, we need to verify your email address.</p>" +
                        "<div style='text-align: center; margin: 30px 0;'>" +
                        "  <a href='%s' style='background-color: %s; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px;'>Verify My Account</a>" +
                        "</div>" +
                        "<p style='font-size: 13px; color: #666;'>This link will expire in 24 hours.</p>",
                getSaccoName(), verifyLink, getPrimaryColor()
        );

        sendHtmlEmail(to, "Verify Your Sacco Account", "Welcome, " + firstName + "!", content);
    }

    @Async
    public void resendVerificationToken(String to, String firstName, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String content = String.format(
                "<p>You requested a new verification link for your <strong>%s</strong> account.</p>" +
                        "<div style='text-align: center; margin: 30px 0;'>" +
                        "  <a href='%s' style='background-color: %s; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Verify Now</a>" +
                        "</div>" +
                        "<p style='font-size: 13px; color: #666;'>Link expires in 24 hours.</p>",
                getSaccoName(), verifyLink, getPrimaryColor()
        );

        sendHtmlEmail(to, "Verify Your Email Address", "Hello " + firstName, content);
    }

    @Async
    public void sendLoanStatusEmail(String to, String loanRef, String status) {
        String color = "APPROVED".equalsIgnoreCase(status) ? "#16a34a" : "#dc2626";
        String content = String.format(
                "<p>Your loan application (Ref: <strong>%s</strong>) has been updated.</p>" +
                        "<div style='text-align: center; padding: 20px; background: #f9fafb; border-radius: 8px; margin: 20px 0;'>" +
                        "  <span style='font-size: 14px; color: #6b7280; text-transform: uppercase; letter-spacing: 1px;'>New Status</span><br>" +
                        "  <span style='font-size: 24px; font-weight: bold; color: %s;'>%s</span>" +
                        "</div>" +
                        "<p>Please log in to the portal to view the full details/repayment schedule.</p>",
                loanRef, color, status
        );

        sendHtmlEmail(to, "Loan Status Update", "Loan #" + loanRef, content);
    }

    @Async
    public void sendMemberWelcomeEmail(String to, String name, String tempPassword, String token) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + token;
        String content = String.format(
                "<p>Your member registration is complete. Welcome to <strong>%s</strong>!</p>" +
                        "<div style='background: #f0fdf4; border-left: 4px solid #16a34a; padding: 15px; margin: 20px 0;'>" +
                        "  <p style='margin: 0; font-size: 14px;'><strong>Username:</strong> %s</p>" +
                        "  <p style='margin: 5px 0 0 0;'><strong>Temporary Password:</strong> <span style='font-family: monospace; font-weight: bold;'>%s</span></p>" +
                        "</div>" +
                        "<p>Please verify your email and log in to change your password.</p>" +
                        "<div style='text-align: center; margin: 30px 0;'>" +
                        "  <a href='%s' style='background-color: %s; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Verify & Login</a>" +
                        "</div>",
                getSaccoName(), to, tempPassword, verifyLink, getPrimaryColor()
        );

        sendHtmlEmail(to, "Welcome to " + getSaccoName(), "Welcome, " + name + "!", content);
    }

    @Async
    public void sendPasswordResetEmail(String to, String firstName, String token) {
        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        String content = String.format(
                "<p>We received a request to reset the password for your account.</p>" +
                        "<div style='text-align: center; margin: 30px 0;'>" +
                        "  <a href='%s' style='background-color: %s; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Reset Password</a>" +
                        "</div>" +
                        "<p style='font-size: 13px; color: #666;'>If you didn't request this, you can safely ignore this email.</p>",
                resetLink, getPrimaryColor()
        );

        sendHtmlEmail(to, "Password Reset Request", "Hello " + firstName, content);
    }

    @Async
    public void sendGenericEmail(String to, String subject, String messageBody) {
        sendHtmlEmail(to, subject, "Notification", messageBody.replace("\n", "<br>"));
    }

    // --- CORE HELPER METHODS ---

    private void sendHtmlEmail(String to, String subject, String title, String bodyContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String saccoEmail = systemSettingService.getString("SACCO_EMAIL", fromEmail);
            String displayName = getSaccoName() + " Admin";

            helper.setFrom(displayName + " <" + saccoEmail + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildMasterTemplate(title, bodyContent), true);

            mailSender.send(message);
            log.info("✅ Email sent to {}", to);
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to {}", to, e);
        } catch (Exception e) {
            log.error("❌ Unexpected error sending email to {}", to, e);
        }
    }

    private String buildMasterTemplate(String title, String content) {
        String saccoName = getSaccoName();
        String primaryColor = getPrimaryColor();
        String address = systemSettingService.getString("SACCO_ADDRESS", "Sacco HQ");
        String logoUrl = getLogoUrl();

        return String.format(
                """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#f3f4f6; font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="padding: 40px 20px;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 4px 6px rgba(0,0,0,0.1);">
                                    <tr>
                                        <td style="background: linear-gradient(135deg, %s 0%%, #0f172a 100%%); padding: 40px 30px; text-align: center;">
                                            %s
                                            <h1 style="margin:10px 0 0 0; color:#ffffff; font-size:24px; font-weight:600;">%s</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <h2 style="margin:0 0 20px 0; color:#111827; font-size:20px;">%s</h2>
                                            <div style="font-size:16px; line-height:1.6; color:#4b5563;">
                                                %s
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#f9fafb; padding:30px; text-align:center; border-top:1px solid #e5e7eb; color:#9ca3af; font-size:12px;">
                                            <p>&copy; %d %s. All rights reserved.</p>
                                            <p>%s</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """,
                primaryColor,
                (logoUrl != null ? "<img src='" + logoUrl + "' height='50' style='background:white; padding:5px; border-radius:5px;'><br>" : ""),
                saccoName,
                title,
                content,
                java.time.Year.now().getValue(), saccoName, address
        );
    }

    private String getSaccoName() { return systemSettingService.getString("SACCO_NAME", "Secure Sacco"); }
    private String getPrimaryColor() { return systemSettingService.getString("BRAND_COLOR_PRIMARY", "#059669"); }
    private String getLogoUrl() {
        String logo = systemSettingService.getString("SACCO_LOGO");
        return (logo != null && logo.startsWith("http")) ? logo : null;
    }
}