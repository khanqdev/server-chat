package com.example.chat_server.dto;

import com.example.chat_server.model.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// "Me" of the API contract (section 1)
public record MeResponse(
        String id,
        String displayName,
        String avatarUrl,
        String email,
        boolean emailVerified,
        String bio,
        List<String> authProviders,
        Settings settings,
        Instant createdAt
) {
    public record Settings(String language, String theme, String whoCanMessage, boolean soundEnabled) {}

    public static MeResponse from(User user) {
        List<String> providers = new ArrayList<>();
        if (user.getPassword() != null) {
            providers.add("password");
        }
        if (user.getGoogleId() != null) {
            providers.add("google");
        }
        return new MeResponse(
                user.getId(),
                user.getFullName(),
                null,
                user.getEmail(),
                // Accounts only exist after the email OTP (or Google's email_verified) has been checked
                true,
                null,
                providers,
                // Only the language is stored so far; the rest are the contract defaults until user settings exist
                new Settings(user.getLanguageOrDefault(), "system", "everyone", true),
                user.getCreatedAt());
    }
}
