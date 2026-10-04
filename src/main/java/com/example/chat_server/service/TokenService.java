package com.example.chat_server.service;

import com.example.chat_server.dto.AuthResponse;
import com.example.chat_server.dto.MeResponse;
import com.example.chat_server.exception.ApiException;
import com.example.chat_server.exception.ErrorCode;
import com.example.chat_server.model.RefreshToken;
import com.example.chat_server.model.User;
import com.example.chat_server.repository.RefreshTokenRepository;
import com.example.chat_server.repository.UserRepository;
import com.example.chat_server.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    // Two tabs can refresh at the same moment with the same cookie. A token rotated this recently is
    // answered with a plain TOKEN_EXPIRED instead of being treated as theft.
    private static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    // Body for the client plus the raw refresh token, which the controller puts into the httpOnly cookie
    public record IssuedTokens(AuthResponse response, String refreshToken) {}

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final long refreshExpirationMs;

    public TokenService(JwtTokenProvider jwtTokenProvider,
                        RefreshTokenRepository refreshTokenRepository,
                        UserRepository userRepository,
                        MongoTemplate mongoTemplate,
                        @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public IssuedTokens issueTokens(User user) {
        return issueTokens(user, false);
    }

    public IssuedTokens issueTokens(User user, boolean isNewUser) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokenRepository.save(new RefreshToken(
                sha256(rawRefreshToken), user.getId(), Instant.now().plusMillis(refreshExpirationMs)));

        AuthResponse response = new AuthResponse(
                jwtTokenProvider.generateToken(user.getUsername()),
                jwtTokenProvider.getExpirationSeconds(),
                MeResponse.from(user),
                isNewUser ? Boolean.TRUE : null);
        return new IssuedTokens(response, rawRefreshToken);
    }

    // Rotation: each refresh token is single-use and is replaced by a new pair
    public IssuedTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw expired();
        }
        String hash = sha256(rawRefreshToken);
        Instant now = Instant.now();

        // Atomic: of several concurrent requests with the same token, only one gets the document back
        RefreshToken stored = mongoTemplate.findAndModify(
                Query.query(Criteria.where("tokenHash").is(hash)
                        .and("revokedAt").isNull()
                        .and("expiresAt").gt(now)),
                new Update().set("revokedAt", now),
                RefreshToken.class);

        if (stored == null) {
            refreshTokenRepository.findByTokenHash(hash)
                    .filter(token -> token.getRevokedAt() != null
                            && token.getRevokedAt().isBefore(now.minus(REUSE_GRACE)))
                    .ifPresent(token -> {
                        // An already rotated token came back: assume it was stolen and end every session
                        log.warn("Refresh token reuse detected for user {}", token.getUserId());
                        refreshTokenRepository.deleteByUserId(token.getUserId());
                        throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_REUSED,
                                "Refresh token was already used; all sessions have been signed out");
                    });
            throw expired();
        }

        User user = userRepository.findById(stored.getUserId()).orElseThrow(TokenService::expired);
        return issueTokens(user);
    }

    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenRepository.deleteByTokenHash(sha256(rawRefreshToken));
        }
    }

    public void revokeAll(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    public Duration refreshTokenLifetime() {
        return Duration.ofMillis(refreshExpirationMs);
    }

    private static ApiException expired() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_EXPIRED,
                "Refresh token is missing, invalid or expired");
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
