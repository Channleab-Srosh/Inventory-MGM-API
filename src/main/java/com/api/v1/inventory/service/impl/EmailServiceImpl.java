package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Override
    public void sendPasswordResetEmail(String toEmail, String otpOrToken) {
        log.info("Generating password reset OTP for [{}]: {}", toEmail, otpOrToken);

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
                        + "This code will expire in 15 minutes.\n"
                        + "If you did not make this request, please ignore this email.");

                mailSender.send(message);
                log.info("Password reset email sent successfully to {}", toEmail);
            } catch (Exception e) {
                log.warn("Could not dispatch email via SMTP (using fallback log): {}", e.getMessage());
            }
        } else {
            log.info("SMTP mail sender not fully configured; check logs above for the generated OTP.");
        }
    }
}
