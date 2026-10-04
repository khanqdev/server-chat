package com.example.chat_server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// The refresh token is never part of the body: it travels in an httpOnly cookie
public record AuthResponse(
        String accessToken,
        long accessTokenExpiresIn,
        MeResponse user,
        // Only present (true) when the account was just created through Google
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean isNewUser
) {}
