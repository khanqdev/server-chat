package com.example.chat_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        String email,

        // BCrypt only uses the first 72 bytes
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8-72 characters")
        String password,

        @NotBlank(message = "displayName is required")
        @Size(min = 2, max = 50, message = "displayName must be 2-50 characters")
        String displayName,

        // Optional; decides the language of the OTP email and the initial user setting
        @Pattern(regexp = "^(vi|en)$", message = "language must be vi or en")
        String language
) {}
