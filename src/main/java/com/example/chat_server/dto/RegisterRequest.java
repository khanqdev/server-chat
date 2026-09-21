package com.example.chat_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username là bắt buộc")
        @Pattern(regexp = "^[a-zA-Z0-9._]{3,30}$",
                message = "Username dài 3-30 ký tự, chỉ gồm chữ, số, dấu chấm và gạch dưới")
        String username,

        @NotBlank(message = "Email là bắt buộc")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotBlank(message = "Họ và tên là bắt buộc")
        @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
        String fullName,

        // BCrypt only uses the first 72 bytes
        @NotBlank(message = "Mật khẩu là bắt buộc")
        @Size(min = 8, max = 72, message = "Mật khẩu dài 8-72 ký tự")
        String password
) {}
