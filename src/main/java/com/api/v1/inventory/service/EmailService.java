package com.api.v1.inventory.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String otpOrToken);

    void sendPasswordResetEmail(String toEmail, String recipientName, String otpOrToken);

    /**
     * Sends the registration OTP email.
     *
     * @param toEmail       recipient email address
     * @param recipientName display name shown in the greeting (use the email
     *                      address if no real name is known yet)
     * @param rawOtp        the plain 6-digit OTP (never persisted; only used
     *                      here to compose the message)
     */
    void sendRegistrationOtpEmail(String toEmail, String recipientName, String rawOtp);
}
