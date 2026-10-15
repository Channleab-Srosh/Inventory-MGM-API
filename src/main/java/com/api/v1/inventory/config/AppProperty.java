package com.api.v1.inventory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperty {

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
        private int pendingExpiration;
        private int verifiedExpiration;
        private int maxRequests;
        private int limitWindow;
    }
}
