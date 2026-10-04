package com.example.chat_server.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// Registration waiting for OTP confirmation; the real User is created only after the OTP is verified
@Document(collection = "pending_registrations")
public class PendingRegistration {
    // Email is the key, so registering again with the same email replaces the previous attempt
    @Id
    private String email;

    private String fullName;
    private String language;
    private String passwordHash;
    private String otpHash;
    private int attempts;
    private Instant lastSentAt;

    // Set after too many wrong OTPs; until then no new OTP can be requested or verified for this email
    private Instant lockedUntil;

    // MongoDB TTL index removes the document once this time passes (checked roughly every 60s)
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(Instant lastSentAt) { this.lastSentAt = lastSentAt; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
