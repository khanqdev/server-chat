package com.example.chat_server.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token là bắt buộc")
        String refreshToken
) {}
