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
    // Password-reset email (unchanged behaviour, plain-text)
    // -------------------------------------------------------------------------

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String otpOrToken) {
        log.info("Dispatching password reset notification for [{}]", toEmail);

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
        if (mailSender != null && mailFrom != null && !mailFrom.trim().isEmpty()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(toEmail);
                message.setSubject("Password Reset Request - " + SENDER_NAME);
                message.setText("Hello,\n\n"
                        + "You requested a password reset for your account.\n"
                        + "Your password reset code is: " + otpOrToken + "\n\n"
                        + "This code will expire in " + expirationText + ".\n"
                        + "If you did not make this request, please ignore this email.");

                mailSender.send(message);
                log.info("Password reset email sent successfully to {}", toEmail);
            } catch (Exception e) {
                log.warn("Could not dispatch email via SMTP: {}", e.getMessage());
            }
        } else {
            log.info("SMTP mail sender not fully configured; skipping email dispatch.");
        }
    }

    // -------------------------------------------------------------------------
    // Registration OTP email – HTML + plain-text multipart
    // -------------------------------------------------------------------------

    @Async
    @Override
    public void sendRegistrationOtpEmail(String toEmail, String recipientName, String rawOtp) {
        log.info("Dispatching registration OTP notification for [{}]", toEmail);

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
            log.info("SMTP mail sender not fully configured; skipping registration OTP email dispatch.");
            return;
        }

        try {
            // ---- Build HTML body from template ----
            String htmlBody = loadOtpEmailTemplate()
                    .replace("{{NAME}}", escapeHtml(recipientName))
                    .replace("{{OTP_CODE}}", escapeHtml(rawOtp))
                    .replace("{{EXPIRY_TIME}}", escapeHtml(expirationText))
                    .replace("{{EXPIRY_MINUTES}} minutes", escapeHtml(expirationText))
                    .replace("{{EXPIRY_MINUTES}}", escapeHtml(expirationText))
                    .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));

            // ---- Plain-text fallback ----
            String plainText = "Hello, " + recipientName + "\n\n"
                    + "Your Inventory Management System verification code is: " + rawOtp + "\n\n"
                    + "This code expires in " + expirationText + ".\n"
                    + "Do not share it with anyone.\n\n"
                    + "If you did not create an account, please ignore this email.\n\n"
                    + "— " + SENDER_NAME;

            // ---- Subject: «OTP_CODE» is your verification code ----
            String subject = rawOtp + " is your verification code";

            // ---- Compose MimeMessage (multipart/alternative) ----
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(mailFrom, SENDER_NAME));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, htmlBody);   // (text, html)

            mailSender.send(mimeMessage);
            log.info("Registration OTP email sent successfully to {}", toEmail);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.warn("Could not dispatch registration OTP email via SMTP: {}", e.getMessage());
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
                + "<p>Hello, {{NAME}}</p>"
                + "<p>Your verification code is: <strong>{{OTP_CODE}}</strong></p>"
                + "<p>It expires in {{EXPIRY_TIME}}.</p>"
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
