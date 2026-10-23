package com.api.v1.inventory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperty {

    /** OTP expiry expressed in minutes – single source of truth used by both
     *  the backend expiration logic and the email text. */
    public static final int OTP_EXPIRY_MINUTES = 5;

    private Jwt jwt = new Jwt();
    private Otp otp = new Otp();

    @Data
    public static class Jwt {
        private String secret;
        private long accessExpiration;
        private long refreshExpiration;
        private int refreshLimit;
        private int refreshLimitWindow;
    }

    @Data
    public static class Otp {
        /** How long (seconds) an unverified OTP stays valid. Defaults to
         *  {@link AppProperty#OTP_EXPIRY_MINUTES} × 60 when not set. */
        private int pendingExpiration;
        private int verifiedExpiration;
        /** Maximum number of wrong OTP attempts before the code is invalidated. */
        private int maxRequests;
        /** Resend rate-limit window in seconds (e.g. 60 = max 1 resend per minute). */
        private int limitWindow;
    }
}
