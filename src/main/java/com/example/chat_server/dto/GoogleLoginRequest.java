package com.example.chat_server.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "Google ID token là bắt buộc")
        String idToken
) {}
