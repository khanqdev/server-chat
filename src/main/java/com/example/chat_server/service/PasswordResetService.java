package com.example.chat_server.service;

import com.example.chat_server.dto.RegisterResponse;
import com.example.chat_server.dto.ResetPasswordRequest;
import com.example.chat_server.model.PasswordReset;
import com.example.chat_server.model.User;
import com.example.chat_server.repository.PasswordResetRepository;
import com.example.chat_server.repository.UserRepository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import static com.example.chat_server.service.OtpPolicy.LOCK_DURATION;
import static com.example.chat_server.service.OtpPolicy.MAX_OTP_ATTEMPTS;
import static com.example.chat_server.service.OtpPolicy.OTP_TTL;
import static com.example.chat_server.service.OtpPolicy.RESEND_COOLDOWN;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final TokenService tokenService;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final OtpMailService otpMailService;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetRepository passwordResetRepository,
                                TokenService tokenService,
                                MongoTemplate mongoTemplate,
                                PasswordEncoder passwordEncoder,
                                OtpMailService otpMailService) {
        this.userRepository = userRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.tokenService = tokenService;
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
        this.otpMailService = otpMailService;
    }

    // Same response whether or not the email belongs to an account, so the endpoint cannot be used to probe emails
    public RegisterResponse requestReset(String rawEmail) {
        String email = OtpPolicy.normalizeEmail(rawEmail);
        passwordResetRepository.findById(email).ifPresent(existing -> {
            OtpPolicy.ensureNotLocked(existing.getLockedUntil());
            OtpPolicy.ensureCooldownPassed(existing.getLastSentAt());
        });

        String otp = OtpPolicy.newOtp();
        Instant now = Instant.now();

        // Stored even for unknown emails: the cooldown and the later OTP check then behave identically
        PasswordReset reset = new PasswordReset();
        reset.setEmail(email);
        reset.setOtpHash(passwordEncoder.encode(otp));
        reset.setAttempts(0);
        reset.setLastSentAt(now);
        reset.setExpiresAt(now.plus(OTP_TTL));
        passwordResetRepository.save(reset);

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            try {
                otpMailService.sendPasswordResetOtp(email, user.get().getFullName(), otp, OTP_TTL,
                        user.get().getLanguageOrDefault());
            } catch (MailException e) {
                // Otherwise the cooldown would block the user from retrying an OTP they never received
                passwordResetRepository.deleteById(email);
                throw e;
            }
        }

        return new RegisterResponse(email, reset.getExpiresAt(), now.plus(RESEND_COOLDOWN));
    }

    public void resetPassword(ResetPasswordRequest request) {
        String email = OtpPolicy.normalizeEmail(request.email());
        Instant now = Instant.now();

        // Atomically consume one attempt before comparing, so parallel guesses cannot exceed the limit
        PasswordReset reset = mongoTemplate.findAndModify(
                Query.query(Criteria.where("email").is(email)
                        .and("attempts").lt(MAX_OTP_ATTEMPTS)
                        .and("expiresAt").gt(now)),
                new Update().inc("attempts", 1),
                FindAndModifyOptions.options().returnNew(true),
                PasswordReset.class);

        if (reset == null) {
            passwordResetRepository.findById(email).ifPresent(r -> OtpPolicy.ensureNotLocked(r.getLockedUntil()));
            throw OtpPolicy.expired();
        }

        Optional<User> user = userRepository.findByEmail(email);

        // Unknown emails fail exactly like a wrong OTP
        if (user.isEmpty() || !passwordEncoder.matches(request.otp(), reset.getOtpHash())) {
            int attemptsLeft = MAX_OTP_ATTEMPTS - reset.getAttempts();
            if (attemptsLeft > 0) {
                throw OtpPolicy.invalid(attemptsLeft);
            }
            // Keep the document for the whole lock so a new OTP cannot be requested to get around it
            Instant lockedUntil = now.plus(LOCK_DURATION);
            mongoTemplate.updateFirst(Query.query(Criteria.where("email").is(email)),
                    new Update().set("lockedUntil", lockedUntil).set("expiresAt", lockedUntil),
                    PasswordReset.class);
            throw OtpPolicy.locked(lockedUntil);
        }

        // The OTP is single-use: only the request that actually removes the document may change the password
        long removed = mongoTemplate.remove(Query.query(Criteria.where("email").is(email)), PasswordReset.class)
                .getDeletedCount();
        if (removed == 0) {
            throw OtpPolicy.expired();
        }

        // Also gives Google-only accounts a password, and lifts a login lock caused by the forgotten password
        User account = user.get();
        account.setPassword(passwordEncoder.encode(request.newPassword()));
        account.setFailedLoginAttempts(0);
        account.setLoginLockedUntil(null);
        userRepository.save(account);

        // Sign out every device; access tokens already issued stay valid until they expire
        tokenService.revokeAll(account.getId());
    }
}
