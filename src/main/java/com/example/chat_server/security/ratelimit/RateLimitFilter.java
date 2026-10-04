package com.example.chat_server.security.ratelimit;

import com.example.chat_server.exception.ErrorCode;
import com.example.chat_server.exception.GlobalExceptionHandler.ErrorBody;
import tools.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final Set<String> LOCALHOST = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");

    private record Rule(String name, Bandwidth bandwidth) {}

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Cache<String, Bucket> buckets;

    private final Rule general;
    private final Rule login;
    private final Rule register;
    private final Rule otpVerify;
    private final Rule passwordForgot;
    private final Rule passwordReset;

    // e.g. "/api/v1/auth"; endpoint-specific rules are matched relative to it
    private final String authBase;

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper, String apiPrefix) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.authBase = apiPrefix + "/auth";
        this.buckets = Caffeine.newBuilder()
                // Bounded so a flood of spoofed/rotating IPs cannot exhaust memory
                .maximumSize(properties.maxTrackedClients())
                .expireAfterAccess(Duration.ofHours(2))
                .build();

        this.general = rule("general", properties.generalPerMinute(), Duration.ofMinutes(1));
        this.login = rule("login", properties.loginPerMinute(), Duration.ofMinutes(1));
        this.register = rule("register", properties.registerPerHour(), Duration.ofHours(1));
        this.otpVerify = rule("otp-verify", properties.otpVerifyPerHour(), Duration.ofHours(1));
        // Same budgets as registration (sends an email / checks an OTP) but counted separately
        this.passwordForgot = rule("password-forgot", properties.registerPerHour(), Duration.ofHours(1));
        this.passwordReset = rule("password-reset", properties.otpVerifyPerHour(), Duration.ofHours(1));
    }

    private static Rule rule(String name, int capacity, Duration window) {
        return new Rule(name, Bandwidth.builder().capacity(capacity).refillGreedy(capacity, window).build());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.enabled()) {
            return true;
        }
        // SockJS falls back to HTTP polling, which legitimately produces many requests per connection
        return request.getRequestURI().startsWith("/ws-chat");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        if (properties.skipLocalhost() && LOCALHOST.contains(clientIp)) {
            chain.doFilter(request, response);
            return;
        }

        // Endpoint-specific limits sit on top of the general one, so both are consumed
        Rule specific = ruleFor(request);
        if (!consume(general, clientIp, request, response)
                || (specific != null && !consume(specific, clientIp, request, response))) {
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean consume(Rule rule, String clientIp, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ConsumptionProbe probe = buckets
                .get(clientIp + "|" + rule.name(), key -> Bucket.builder().addLimit(rule.bandwidth()).build())
                .tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return true;
        }

        long retryAfter = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
        log.warn("Rate limit '{}' exceeded by {} on {} {}", rule.name(), clientIp,
                request.getMethod(), request.getRequestURI());
        reject(response, retryAfter);
        return false;
    }

    private Rule ruleFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String uri = request.getRequestURI();
        if (!uri.startsWith(authBase + "/")) {
            return null;
        }
        return switch (uri.substring(authBase.length())) {
            case "/login", "/google" -> login;
            // Both endpoints send an email, so they share one budget
            case "/register", "/register/resend-otp" -> register;
            case "/register/verify" -> otpVerify;
            case "/password/forgot" -> passwordForgot;
            case "/password/reset" -> passwordReset;
            default -> null;
        };
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        objectMapper.writeValue(response.getOutputStream(), ErrorBody.of(
                ErrorCode.RATE_LIMITED,
                "Too many requests; retry after " + retryAfterSeconds + " seconds",
                Map.of("retryAfterSec", retryAfterSeconds)));
    }
}
