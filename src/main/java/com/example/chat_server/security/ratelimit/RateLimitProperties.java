package com.example.chat_server.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        // Requests from 127.0.0.1 / ::1 bypass every limit. Must be false in production.
        boolean skipLocalhost,
        int generalPerMinute,
        int loginPerMinute,
        int registerPerHour,
        int otpVerifyPerHour,
        int maxTrackedClients
) {}
