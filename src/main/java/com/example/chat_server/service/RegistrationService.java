package com.example.chat_server.service;

import com.example.chat_server.dto.RegisterRequest;
import com.example.chat_server.dto.RegisterResponse;
import com.example.chat_server.dto.VerifyOtpRequest;
import com.example.chat_server.exception.ApiException;
import com.example.chat_server.exception.ErrorCode;
import com.example.chat_server.model.AuthProvider;
import com.example.chat_server.model.PendingRegistration;
import com.example.chat_server.model.User;
import com.example.chat_server.repository.PendingRegistrationRepository;
import com.example.chat_server.repository.UserRepository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
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
public class RegistrationService {

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final OtpMailService otpMailService;
    private final TokenService tokenService;
    private final UsernameGenerator usernameGenerator;

    public RegistrationService(UserRepository userRepository,
                               PendingRegistrationRepository pendingRepository,
                               MongoTemplate mongoTemplate,
                               PasswordEncoder passwordEncoder,
                               OtpMailService otpMailService,
                               TokenService tokenService,
                               UsernameGenerator usernameGenerator) {
        this.userRepository = userRepository;
        this.pendingRepository = pendingRepository;
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
        this.otpMailService = otpMailService;
        this.tokenService = tokenService;
        this.usernameGenerator = usernameGenerator;
    }

    public RegisterResponse register(RegisterRequest request) {
        String email = OtpPolicy.normalizeEmail(request.email());
        ensureEmailAvailable(email);

        pendingRepository.findById(email).ifPresent(existing -> {
            OtpPolicy.ensureNotLocked(existing.getLockedUntil());
            OtpPolicy.ensureCooldownPassed(existing.getLastSentAt());
        });

        PendingRegistration pending = new PendingRegistration();
        pending.setEmail(email);
        pending.setFullName(request.displayName().trim());
        pending.setLanguage(request.language() != null ? request.language() : "vi");
        pending.setPasswordHash(passwordEncoder.encode(request.password()));

        return sendNewOtp(pending);
    }

    public RegisterResponse resendOtp(String rawEmail) {
        String email = OtpPolicy.normalizeEmail(rawEmail);
        PendingRegistration pending = pendingRepository.findById(email)
                .filter(p -> p.getExpiresAt().isAfter(Instant.now()))
                // Nothing to resend: the client has to start the registration again
                .orElseThrow(OtpPolicy::expired);

        OtpPolicy.ensureNotLocked(pending.getLockedUntil());
        OtpPolicy.ensureCooldownPassed(pending.getLastSentAt());
        return sendNewOtp(pending);
    }

    public TokenService.IssuedTokens verifyOtp(VerifyOtpRequest request) {
        String email = OtpPolicy.normalizeEmail(request.email());
        Instant now = Instant.now();

        // Atomically consume one attempt before comparing, so parallel guesses cannot exceed the limit
        PendingRegistration pending = mongoTemplate.findAndModify(
                Query.query(Criteria.where("email").is(email)
                        .and("attempts").lt(MAX_OTP_ATTEMPTS)
                        .and("expiresAt").gt(now)),
                new Update().inc("attempts", 1),
                FindAndModifyOptions.options().returnNew(true),
                PendingRegistration.class);

        if (pending == null) {
            Optional<PendingRegistration> current = pendingRepository.findById(email);
            current.ifPresent(p -> OtpPolicy.ensureNotLocked(p.getLockedUntil()));
            throw OtpPolicy.expired();
        }

        if (!passwordEncoder.matches(request.otp(), pending.getOtpHash())) {
            int attemptsLeft = MAX_OTP_ATTEMPTS - pending.getAttempts();
            if (attemptsLeft > 0) {
                throw OtpPolicy.invalid(attemptsLeft);
            }
            // Keep the document for the whole lock so a new OTP cannot be requested to get around it
            Instant lockedUntil = now.plus(LOCK_DURATION);
            mongoTemplate.updateFirst(Query.query(Criteria.where("email").is(email)),
                    new Update().set("lockedUntil", lockedUntil).set("expiresAt", lockedUntil),
                    PendingRegistration.class);
            throw OtpPolicy.locked(lockedUntil);
        }

        // Only the request that actually removes the pending document may create the user
        long removed = mongoTemplate.remove(Query.query(Criteria.where("email").is(email)), PendingRegistration.class)
                .getDeletedCount();
        if (removed == 0) {
            throw OtpPolicy.expired();
        }

        ensureEmailAvailable(email);

        User user = new User();
        user.setUsername(usernameGenerator.fromEmail(email));
        user.setEmail(email);
        user.setFullName(pending.getFullName());
        user.setLanguage(pending.getLanguage());
        user.setPassword(pending.getPasswordHash());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setCreatedAt(now);
        userRepository.save(user);

        return tokenService.issueTokens(user);
    }

    private RegisterResponse sendNewOtp(PendingRegistration pending) {
        String otp = OtpPolicy.newOtp();
        Instant now = Instant.now();

        pending.setOtpHash(passwordEncoder.encode(otp));
        pending.setAttempts(0);
        pending.setLockedUntil(null);
        pending.setLastSentAt(now);
        pending.setExpiresAt(now.plus(OTP_TTL));
        pendingRepository.save(pending);

        try {
            otpMailService.sendRegistrationOtp(pending.getEmail(), pending.getFullName(), otp, OTP_TTL,
                    pending.getLanguage());
        } catch (MailException e) {
            // Otherwise the cooldown would block the user from retrying an OTP they never received
            pendingRepository.deleteById(pending.getEmail());
            throw e;
        }
        return new RegisterResponse(pending.getEmail(), pending.getExpiresAt(), now.plus(RESEND_COOLDOWN));
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.EMAIL_TAKEN, "Email is already registered");
        }
    }
}
