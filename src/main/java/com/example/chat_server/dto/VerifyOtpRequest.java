package com.example.chat_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "Email là bắt buộc")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotBlank(message = "OTP là bắt buộc")
        @Pattern(regexp = "^\\d{6}$", message = "OTP gồm 6 chữ số")
        String otp
) {}
