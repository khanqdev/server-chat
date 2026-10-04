package com.example.chat_server.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// The refresh token lives in an httpOnly cookie so JavaScript (and an XSS bug) can never read it
@Component
public class RefreshCookie {

    public static final String NAME = "refresh_token";

    private final String path;
    private final boolean secure;
    private final Duration maxAge;

    public RefreshCookie(@Value("${app.api.prefix}") String apiPrefix,
                         @Value("${app.cookie.secure}") boolean secure,
                         @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        // Only sent to the auth endpoints, never with ordinary API calls
        this.path = apiPrefix + "/auth";
        this.secure = secure;
        this.maxAge = Duration.ofMillis(refreshExpirationMs);
    }

    public void set(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(refreshToken, maxAge).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    // No Domain attribute: the cookie belongs to whatever host the browser called (e.g. the Vercel proxy)
    private ResponseCookie build(String value, Duration age) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(path)
                .maxAge(age)
                .build();
    }
}
