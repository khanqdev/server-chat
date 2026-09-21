package com.example.chat_server.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username là bắt buộc")
        String username,

        @NotBlank(message = "Mật khẩu là bắt buộc")
        String password
) {}
