package com.example.chat_server.service;

import com.example.chat_server.dto.ForgotPasswordResponse;
import com.example.chat_server.dto.ResetPasswordRequest;
import com.example.chat_server.exception.ApiException;
import com.example.chat_server.model.PasswordReset;
import com.example.chat_server.model.User;
import com.example.chat_server.repository.PasswordResetRepository;
import com.example.chat_server.repository.RefreshTokenRepository;
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
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final OtpMailService otpMailService;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetRepository passwordResetRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                MongoTemplate mongoTemplate,
                                PasswordEncoder passwordEncoder,
                                OtpMailService otpMailService) {
        this.userRepository = userRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
        this.otpMailService = otpMailService;
    }

    // Same response whether or not the email belongs to an account, so the endpoint cannot be used to probe emails
    public ForgotPasswordResponse requestReset(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        passwordResetRepository.findById(email).ifPresent(this::ensureCooldownPassed);

        String otp = "%06d".formatted(RANDOM.nextInt(1_000_000));
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
                otpMailService.sendPasswordResetOtp(email, user.get().getFullName(), otp, OTP_TTL);
            } catch (MailException e) {
                // Otherwise the cooldown would block the user from retrying an OTP they never received
                passwordResetRepository.deleteById(email);
                throw e;
            }
        }

        return new ForgotPasswordResponse(
                "Nếu email thuộc về một tài khoản, mã OTP đặt lại mật khẩu đã được gửi",
                email, OTP_TTL.toSeconds(), RESEND_COOLDOWN.toSeconds());
    }

    public void resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.email());
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
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "OTP đã hết hạn hoặc bạn đã nhập sai quá nhiều lần, vui lòng yêu cầu gửi lại OTP");
        }

        Optional<User> user = userRepository.findByEmail(email);

        // Unknown emails fail exactly like a wrong OTP
        if (user.isEmpty() || !passwordEncoder.matches(request.otp(), reset.getOtpHash())) {
            int remaining = MAX_OTP_ATTEMPTS - reset.getAttempts();
            throw new ApiException(HttpStatus.BAD_REQUEST, remaining > 0
                    ? "OTP không đúng, bạn còn " + remaining + " lần thử"
                    : "OTP không đúng, bạn đã hết lượt thử, vui lòng yêu cầu gửi lại OTP");
        }

        // The OTP is single-use: only the request that actually removes the document may change the password
        long removed = mongoTemplate.remove(Query.query(Criteria.where("email").is(email)), PasswordReset.class)
                .getDeletedCount();
        if (removed == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP đã được sử dụng");
        }

        // Also gives Google-only accounts a password, so they can sign in either way afterwards
        User account = user.get();
        account.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(account);

        // Sign out every device; access tokens already issued stay valid until they expire
        refreshTokenRepository.deleteByUserId(account.getId());
    }

    private void ensureCooldownPassed(PasswordReset reset) {
        Instant nextAllowed = reset.getLastSentAt().plus(RESEND_COOLDOWN);
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
