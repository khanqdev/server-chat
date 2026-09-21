package com.example.chat_server.service;

import com.example.chat_server.dto.AuthResponse;
import com.example.chat_server.exception.ApiException;
import com.example.chat_server.model.AuthProvider;
import com.example.chat_server.model.User;
import com.example.chat_server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);
    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final String clientId;
    private final JwtDecoder googleIdTokenDecoder;

    public GoogleAuthService(UserRepository userRepository,
                             TokenService tokenService,
                             @Value("${app.google.client-id:}") String clientId) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.clientId = clientId;

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS,
                        iss -> iss != null && GOOGLE_ISSUERS.contains(iss.toString())),
                // Without the audience check, an ID token issued to ANY Google app would be accepted
                new JwtClaimValidator<Collection<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(clientId))));
        this.googleIdTokenDecoder = decoder;
    }

    public AuthResponse login(String idToken) {
        if (clientId.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Đăng nhập Google chưa được cấu hình (thiếu GOOGLE_CLIENT_ID)");
        }

        Jwt jwt;
        try {
            jwt = googleIdTokenDecoder.decode(idToken);
        } catch (JwtException e) {
            log.warn("Rejected Google ID token: {}", e.getMessage());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google ID token không hợp lệ hoặc đã hết hạn");
        }

        String rawEmail = jwt.getClaimAsString("email");
        if (rawEmail == null || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email của tài khoản Google chưa được xác minh");
        }
        String email = rawEmail.toLowerCase(Locale.ROOT);
        String googleId = jwt.getSubject();

        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email).map(existing -> linkGoogle(existing, googleId)))
                .orElseGet(() -> createGoogleUser(jwt, email, googleId));

        return tokenService.issueTokens(user);
    }

    // Both sides have verified ownership of the email (OTP locally, email_verified at Google), so linking is safe
    private User linkGoogle(User existing, String googleId) {
        if (existing.getGoogleId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Email này đã được liên kết với một tài khoản Google khác");
        }
        existing.setGoogleId(googleId);
        return userRepository.save(existing);
    }

    private User createGoogleUser(Jwt jwt, String email, String googleId) {
        String fullName = jwt.getClaimAsString("name");

        User user = new User();
        user.setUsername(generateUsername(email));
        user.setEmail(email);
        user.setFullName(fullName != null && !fullName.isBlank() ? fullName : email.substring(0, email.indexOf('@')));
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setGoogleId(googleId);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private String generateUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-z0-9._]", "");
        if (base.length() < 3) {
            base = base + "user";
        }
        base = base.substring(0, Math.min(base.length(), 25));

        String candidate = base;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + (1000 + RANDOM.nextInt(9000));
        }
        return candidate;
    }
}
