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

    /**
     * Sends the welcome email with initial account credentials.
     *
     * @param toEmail       recipient email address
     * @param recipientName display name of the user
     * @param username      account username for login
     * @param rawPassword   initial plain-text password
     * @param role          assigned role name (e.g. ADMIN, MANAGER, STAFF)
     */
    void sendNewUserCredentialsEmail(String toEmail, String recipientName, String username, String rawPassword, String role);
}
