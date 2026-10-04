package com.example.chat_server.service;

import com.example.chat_server.dto.LoginRequest;
import com.example.chat_server.exception.ApiException;
import com.example.chat_server.exception.ErrorCode;
import com.example.chat_server.model.User;
import com.example.chat_server.repository.UserRepository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class AuthService {

    private static final int MAX_FAILED_LOGINS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository,
                       MongoTemplate mongoTemplate,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public TokenService.IssuedTokens login(LoginRequest request) {
        // Unknown email and Google-only account answer exactly like a wrong password
        User user = userRepository.findByEmail(OtpPolicy.normalizeEmail(request.email()))
                .orElseThrow(AuthService::invalidCredentials);

        Instant now = Instant.now();
        if (user.getLoginLockedUntil() != null && user.getLoginLockedUntil().isAfter(now)) {
            throw locked(user.getLoginLockedUntil());
        }

        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw registerFailure(user.getId(), now);
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLoginLockedUntil() != null) {
            mongoTemplate.updateFirst(byId(user.getId()),
                    new Update().set("failedLoginAttempts", 0).unset("loginLockedUntil"), User.class);
        }
        return tokenService.issueTokens(user);
    }

    // Counts atomically so parallel guesses cannot get more than MAX_FAILED_LOGINS tries
    private ApiException registerFailure(String userId, Instant now) {
        User updated = mongoTemplate.findAndModify(byId(userId),
                new Update().inc("failedLoginAttempts", 1),
                FindAndModifyOptions.options().returnNew(true), User.class);

        if (updated != null && updated.getFailedLoginAttempts() >= MAX_FAILED_LOGINS) {
            Instant lockedUntil = now.plus(LOCK_DURATION);
            mongoTemplate.updateFirst(byId(userId),
                    new Update().set("failedLoginAttempts", 0).set("loginLockedUntil", lockedUntil), User.class);
            return locked(lockedUntil);
        }
        return invalidCredentials();
    }

    private static Query byId(String userId) {
        return Query.query(Criteria.where("_id").is(userId));
    }

    private static ApiException locked(Instant lockedUntil) {
        return new ApiException(HttpStatus.LOCKED, ErrorCode.AUTH_LOCKED, "Too many failed sign-in attempts",
                Map.of("retryAfterSec", OtpPolicy.secondsUntil(lockedUntil)));
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_INVALID_CREDENTIALS,
                "Incorrect email or password");
    }
}
