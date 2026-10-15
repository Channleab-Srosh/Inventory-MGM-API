package com.api.v1.inventory.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String otpOrToken);
}
