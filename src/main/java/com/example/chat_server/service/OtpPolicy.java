package com.example.chat_server.service;

import com.example.chat_server.exception.ApiException;
import com.example.chat_server.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

// OTP rules shared by registration and password reset (contract: 6 digits, 5 minutes, 5 wrong tries -> 15 minutes lock)
final class OtpPolicy {
    static final Duration OTP_TTL = Duration.ofMinutes(5);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    static final int MAX_OTP_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpPolicy() {}

    static String newOtp() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static void ensureNotLocked(Instant lockedUntil) {
        if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
            throw locked(lockedUntil);
        }
    }

    static void ensureCooldownPassed(Instant lastSentAt) {
        Instant nextAllowed = lastSentAt.plus(RESEND_COOLDOWN);
        if (Instant.now().isBefore(nextAllowed)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMITED,
                    "A new OTP can only be requested every " + RESEND_COOLDOWN.toSeconds() + " seconds",
                    Map.of("retryAfterSec", secondsUntil(nextAllowed)));
        }
    }

    static ApiException locked(Instant lockedUntil) {
        return new ApiException(HttpStatus.LOCKED, ErrorCode.OTP_LOCKED, "Too many wrong OTP attempts",
                Map.of("retryAfterSec", secondsUntil(lockedUntil)));
    }

    static ApiException expired() {
        return new ApiException(HttpStatus.GONE, ErrorCode.OTP_EXPIRED, "OTP has expired or was never requested");
    }

    static ApiException invalid(int attemptsLeft) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.OTP_INVALID, "OTP is incorrect",
                Map.of("attemptsLeft", attemptsLeft));
    }

    static long secondsUntil(Instant instant) {
        return Math.max(1, Duration.between(Instant.now(), instant).toSeconds() + 1);
    }
}
