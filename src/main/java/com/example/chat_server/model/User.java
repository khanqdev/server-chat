package com.example.chat_server.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
public class User {
    @Id
    private String id;

    // Internal handle generated from the email; it is the JWT subject and the STOMP user name.
    // The REST API identifies people by email / id and shows fullName as "displayName".
    @Indexed(unique = true)
    private String username;

    // sparse: accounts created before email became mandatory have no email
    @Indexed(unique = true, sparse = true)
    private String email;

    private String fullName;

    // Null for accounts created through Google
    private String password;

    // How the account was originally created
    private AuthProvider authProvider;

    @Indexed(unique = true, sparse = true)
    private String googleId;

    // "vi" or "en"; null on accounts created before the setting existed
    private String language;

    // Consecutive wrong passwords; 5 in a row lock the login for 15 minutes
    private int failedLoginAttempts;
    private Instant loginLockedUntil;

    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getLanguageOrDefault() { return language != null ? language : "vi"; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public Instant getLoginLockedUntil() { return loginLockedUntil; }
    public void setLoginLockedUntil(Instant loginLockedUntil) { this.loginLockedUntil = loginLockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
