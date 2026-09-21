package com.example.chat_server.service;

import com.example.chat_server.dto.AuthResponse;
import com.example.chat_server.exception.ApiException;
import com.example.chat_server.model.RefreshToken;
import com.example.chat_server.model.User;
import com.example.chat_server.repository.RefreshTokenRepository;
import com.example.chat_server.repository.UserRepository;
import com.example.chat_server.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshExpirationMs;

    public TokenService(JwtTokenProvider jwtTokenProvider,
                        RefreshTokenRepository refreshTokenRepository,
                        UserRepository userRepository,
                        @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public AuthResponse issueTokens(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokenRepository.save(new RefreshToken(
                sha256(rawRefreshToken), user.getId(), Instant.now().plusMillis(refreshExpirationMs)));

        return new AuthResponse(
                jwtTokenProvider.generateToken(user.getUsername()),
                rawRefreshToken,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                AuthResponse.UserInfo.from(user));
    }

    // Rotation: each refresh token is single-use and is replaced by a new pair
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash).orElseThrow(TokenService::invalid);

        // Delete count guards against two concurrent refreshes both using the same token
        if (refreshTokenRepository.deleteByTokenHash(hash) == 0 || stored.getExpiresAt().isBefore(Instant.now())) {
            throw invalid();
        }

        User user = userRepository.findById(stored.getUserId()).orElseThrow(TokenService::invalid);
        return issueTokens(user);
    }

    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.deleteByTokenHash(sha256(rawRefreshToken));
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
