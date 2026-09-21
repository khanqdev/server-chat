package com.example.chat_server.dto;

public record RegisterResponse(String message, String email, long otpExpiresInSeconds) {}
