package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.config.AppProperty;
import com.api.v1.inventory.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Year;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String SENDER_NAME = "Inventory Management System";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppProperty appProperty;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    // -------------------------------------------------------------------------
    // Password-reset email – HTML + plain-text multipart (matching Register OTP form)
    // -------------------------------------------------------------------------

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String otpOrToken) {
        sendPasswordResetEmail(toEmail, toEmail, otpOrToken);
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String otpOrToken) {
        log.info("Dispatching password reset notification for [{}]", toEmail);
        sendOtpEmail(
                toEmail,
                recipientName,
                otpOrToken,
                "Reset Password OTP",
                "You requested a password reset for your account. Use the verification code below to reset your password:",
                "If you did not request a password reset, please ignore this email. No action is required.",
                otpOrToken + " is your reset password OTP",
                "Password reset OTP"
        );
    }

    // -------------------------------------------------------------------------
    // Registration OTP email – HTML + plain-text multipart
    // -------------------------------------------------------------------------

    @Async
    @Override
    public void sendRegistrationOtpEmail(String toEmail, String recipientName, String rawOtp) {
        log.info("Dispatching registration OTP notification for [{}]", toEmail);
        sendOtpEmail(
                toEmail,
                recipientName,
                rawOtp,
                "Register OTP",
                "Thank you for registering. Use the verification code below to complete your account setup:",
                "If you did not create an account with Inventory Management System, please ignore this email. No action is required.",
                rawOtp + " is your register OTP",
                "Registration OTP"
        );
    }

    private void sendOtpEmail(String toEmail,
                              String recipientName,
                              String rawOtp,
                              String title,
                              String messageText,
                              String warningText,
                              String subject,
                              String operationLabel) {
        int expirationSeconds = appProperty.getOtp().getPendingExpiration() > 0
                ? appProperty.getOtp().getPendingExpiration()
                : AppProperty.OTP_EXPIRY_SECONDS;

        String expirationText;
        if (expirationSeconds >= 60 && expirationSeconds % 60 == 0) {
            expirationText = (expirationSeconds / 60) + " minutes";
        } else {
            expirationText = expirationSeconds + " seconds";
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || mailFrom == null || mailFrom.trim().isEmpty()) {
            log.info("SMTP mail sender not fully configured; skipping {} email dispatch.", operationLabel);
            return;
        }

        try {
            // ---- Build HTML body from template ----
            String htmlBody = loadOtpEmailTemplate()
                    .replace("{{TITLE}}", escapeHtml(title))
                    .replace("{{NAME}}", escapeHtml(recipientName))
                    .replace("{{MESSAGE}}", escapeHtml(messageText))
                    .replace("{{OTP_CODE}}", escapeHtml(rawOtp))
                    .replace("{{EXPIRY_TIME}}", escapeHtml(expirationText))
                    .replace("{{EXPIRY_MINUTES}} minutes", escapeHtml(expirationText))
                    .replace("{{EXPIRY_MINUTES}}", escapeHtml(expirationText))
                    .replace("{{WARNING}}", escapeHtml(warningText))
                    .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));

            // ---- Plain-text fallback ----
            String plainText = "Hello, " + recipientName + "\n\n"
                    + messageText + "\n\n"
                    + "Your Inventory Management System verification code is: " + rawOtp + "\n\n"
                    + "This code expires in " + expirationText + ".\n"
                    + "Do not share it with anyone.\n\n"
                    + warningText + "\n\n"
                    + "— " + SENDER_NAME;

            // ---- Compose MimeMessage (multipart/alternative) ----
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(mailFrom, SENDER_NAME));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, htmlBody);   // (text, html)

            mailSender.send(mimeMessage);
            log.info("{} email sent successfully to {}", operationLabel, toEmail);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.warn("Could not dispatch {} email via SMTP: {}", operationLabel, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Loads {@code templates/otp-email.html} from the classpath.
     * Falls back to a minimal inline template if the file is not found.
     */
    private String loadOtpEmailTemplate() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("templates/otp-email.html")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Could not load otp-email.html template: {}", e.getMessage());
        }
        // Minimal inline fallback
        return "<html><body>"
                + "<h2>{{TITLE}}</h2>"
                + "<p>Hello, {{NAME}}</p>"
                + "<p>{{MESSAGE}}</p>"
                + "<p>Your verification code is: <strong>{{OTP_CODE}}</strong></p>"
                + "<p>It expires in {{EXPIRY_TIME}}.</p>"
                + "<p>{{WARNING}}</p>"
                + "<p>&copy; {{YEAR}} Inventory Management System</p>"
                + "</body></html>";
    }

    /** Escapes characters that are unsafe inside HTML text nodes. */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
