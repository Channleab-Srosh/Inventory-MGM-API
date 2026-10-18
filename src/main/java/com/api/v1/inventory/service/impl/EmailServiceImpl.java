package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.config.AppProperty;
import com.api.v1.inventory.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppProperty appProperty;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String otpOrToken) {
        log.info("Dispatching password reset notification for [{}]", toEmail);

        int expirationSeconds = appProperty.getOtp().getPendingExpiration() > 0
                ? appProperty.getOtp().getPendingExpiration()
                : 300;

        String expirationText;
        if (expirationSeconds >= 60 && expirationSeconds % 60 == 0) {
            expirationText = (expirationSeconds / 60) + " minutes";
        } else if (expirationSeconds >= 60) {
            expirationText = expirationSeconds + " seconds";
        } else {
            expirationText = expirationSeconds + " seconds";
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null && mailFrom != null && !mailFrom.trim().isEmpty()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(toEmail);
                message.setSubject("Password Reset Request - Inventory Management System");
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
}
