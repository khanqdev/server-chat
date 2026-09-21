package com.example.chat_server.service;

import com.example.chat_server.dto.AuthResponse;
import com.example.chat_server.dto.RegisterRequest;
import com.example.chat_server.dto.RegisterResponse;
import com.example.chat_server.dto.VerifyOtpRequest;
import com.example.chat_server.exception.ApiException;
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

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class RegistrationService {

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final OtpMailService otpMailService;
    private final TokenService tokenService;

    public RegistrationService(UserRepository userRepository,
                               PendingRegistrationRepository pendingRepository,
                               MongoTemplate mongoTemplate,
                               PasswordEncoder passwordEncoder,
                               OtpMailService otpMailService,
                               TokenService tokenService) {
        this.userRepository = userRepository;
        this.pendingRepository = pendingRepository;
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
        this.otpMailService = otpMailService;
        this.tokenService = tokenService;
    }

    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String username = request.username().trim();
        ensureAvailable(username, email);

        pendingRepository.findById(email).ifPresent(this::ensureCooldownPassed);

        PendingRegistration pending = new PendingRegistration();
        pending.setEmail(email);
        pending.setUsername(username);
        pending.setFullName(request.fullName().trim());
        pending.setPasswordHash(passwordEncoder.encode(request.password()));

        sendNewOtp(pending);
        return new RegisterResponse("Đã gửi mã OTP tới email của bạn", email, OTP_TTL.toSeconds());
    }

    public RegisterResponse resendOtp(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        PendingRegistration pending = pendingRepository.findById(email)
                .filter(p -> p.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Không có yêu cầu đăng ký nào đang chờ cho email này, vui lòng đăng ký lại"));

        ensureCooldownPassed(pending);
        sendNewOtp(pending);
        return new RegisterResponse("Đã gửi lại mã OTP tới email của bạn", email, OTP_TTL.toSeconds());
    }

    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String email = normalizeEmail(request.email());
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
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "OTP đã hết hạn hoặc bạn đã nhập sai quá nhiều lần, vui lòng yêu cầu gửi lại OTP");
        }

        if (!passwordEncoder.matches(request.otp(), pending.getOtpHash())) {
            int remaining = MAX_OTP_ATTEMPTS - pending.getAttempts();
            throw new ApiException(HttpStatus.BAD_REQUEST, remaining > 0
                    ? "OTP không đúng, bạn còn " + remaining + " lần thử"
                    : "OTP không đúng, bạn đã hết lượt thử, vui lòng yêu cầu gửi lại OTP");
        }

        // Only the request that actually removes the pending document may create the user
        long removed = mongoTemplate.remove(Query.query(Criteria.where("email").is(email)), PendingRegistration.class)
                .getDeletedCount();
        if (removed == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP đã được sử dụng");
        }

        ensureAvailable(pending.getUsername(), email);

        User user = new User();
        user.setUsername(pending.getUsername());
        user.setEmail(email);
        user.setFullName(pending.getFullName());
        user.setPassword(pending.getPasswordHash());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setCreatedAt(now);
        userRepository.save(user);

        return tokenService.issueTokens(user);
    }

    private void sendNewOtp(PendingRegistration pending) {
        String otp = "%06d".formatted(RANDOM.nextInt(1_000_000));
        Instant now = Instant.now();

        pending.setOtpHash(passwordEncoder.encode(otp));
        pending.setAttempts(0);
        pending.setLastSentAt(now);
        pending.setExpiresAt(now.plus(OTP_TTL));
        pendingRepository.save(pending);

        try {
            otpMailService.sendRegistrationOtp(pending.getEmail(), pending.getFullName(), otp, OTP_TTL);
        } catch (MailException e) {
            // Otherwise the cooldown would block the user from retrying an OTP they never received
            pendingRepository.deleteById(pending.getEmail());
            throw e;
        }
    }

    private void ensureAvailable(String username, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username đã được sử dụng");
        }
    }

    private void ensureCooldownPassed(PendingRegistration pending) {
        Instant nextAllowed = pending.getLastSentAt().plus(RESEND_COOLDOWN);
        if (Instant.now().isBefore(nextAllowed)) {
            long wait = Duration.between(Instant.now(), nextAllowed).toSeconds() + 1;
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Vui lòng đợi " + wait + " giây trước khi yêu cầu OTP mới");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
