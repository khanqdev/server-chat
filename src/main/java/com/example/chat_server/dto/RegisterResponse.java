package com.example.chat_server.dto;

import java.time.Instant;

// 202 body of every endpoint that emails an OTP (register, resend, forgot password)
public record RegisterResponse(String email, Instant otpExpiresAt, Instant resendAvailableAt) {}
