package com.example.chat_server.dto;

import com.example.chat_server.model.AuthProvider;
import com.example.chat_server.model.User;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresInSeconds,
        UserInfo user
) {
    public record UserInfo(String id, String username, String email, String fullName, AuthProvider authProvider) {
        public static UserInfo from(User user) {
            return new UserInfo(user.getId(), user.getUsername(), user.getEmail(),
                    user.getFullName(), user.getAuthProvider());
        }
    }
}
