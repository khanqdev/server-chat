package com.example.chat_server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OtpMailService {

    private static final Logger log = LoggerFactory.getLogger(OtpMailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String mailHost;
    private final String from;

    public OtpMailService(ObjectProvider<JavaMailSender> mailSender,
                          @Value("${spring.mail.host:}") String mailHost,
                          @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.from = from;
    }

    // language is "vi" or "en" (the user's setting); anything else falls back to Vietnamese
    public void sendRegistrationOtp(String email, String fullName, String otp, Duration ttl, String language) {
        if ("en".equals(language)) {
            send(email, "registration", otp, "Your Chat verification code", """
                    Hi %s,

                    Your sign-up verification code is: %s

                    The code is valid for %d minutes. Do not share it with anyone.
                    If you did not sign up, you can ignore this email.
                    """.formatted(fullName, otp, ttl.toMinutes()));
        } else {
            send(email, "registration", otp, "Mã xác thực đăng ký tài khoản Chat", """
                    Xin chào %s,

                    Mã OTP đăng ký tài khoản của bạn là: %s

                    Mã có hiệu lực trong %d phút. Không chia sẻ mã này cho bất kỳ ai.
                    Nếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.
                    """.formatted(fullName, otp, ttl.toMinutes()));
        }
    }

    public void sendPasswordResetOtp(String email, String fullName, String otp, Duration ttl, String language) {
        if ("en".equals(language)) {
            send(email, "password reset", otp, "Your Chat password reset code", """
                    Hi %s,

                    Your password reset code is: %s

                    The code is valid for %d minutes. Do not share it with anyone.
                    If you did not ask to reset your password, ignore this email; your password stays the same.
                    """.formatted(fullName, otp, ttl.toMinutes()));
        } else {
            send(email, "password reset", otp, "Mã đặt lại mật khẩu tài khoản Chat", """
                    Xin chào %s,

                    Mã OTP đặt lại mật khẩu của bạn là: %s

                    Mã có hiệu lực trong %d phút. Không chia sẻ mã này cho bất kỳ ai.
                    Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này; mật khẩu của bạn không thay đổi.
                    """.formatted(fullName, otp, ttl.toMinutes()));
        }
    }

    private void send(String email, String purpose, String otp, String subject, String text) {
        // Local development without SMTP: print the OTP instead of failing. Never leave MAIL_HOST empty in production.
        if (mailHost.isBlank()) {
            log.warn("[DEV] MAIL_HOST chưa cấu hình, không gửi email. OTP ({}) cho {}: {}", purpose, email, otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(text);
        mailSender.getObject().send(message);
    }
}
