package com.example.chat_server.exception;

// Machine-readable error codes of the API contract; the client translates them, the server never sends UI text
public final class ErrorCode {
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String OTP_INVALID = "OTP_INVALID";
    public static final String OTP_EXPIRED = "OTP_EXPIRED";
    public static final String OTP_LOCKED = "OTP_LOCKED";
    public static final String TOKEN_REUSED = "TOKEN_REUSED";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String AUTH_LOCKED = "AUTH_LOCKED";
    public static final String GOOGLE_TOKEN_INVALID = "GOOGLE_TOKEN_INVALID";
    public static final String ACCOUNT_LINK_REQUIRED = "ACCOUNT_LINK_REQUIRED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String EMAIL_TAKEN = "EMAIL_TAKEN";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ErrorCode() {}
}
