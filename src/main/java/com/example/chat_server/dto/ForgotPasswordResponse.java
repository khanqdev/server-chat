package com.example.chat_server.dto;

public record ForgotPasswordResponse(String message, String email, long otpExpiresInSeconds, long resendAvailableInSeconds) {}
