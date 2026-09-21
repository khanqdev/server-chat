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

    public void sendRegistrationOtp(String email, String fullName, String otp, Duration ttl) {
        // Local development without SMTP: print the OTP instead of failing. Never leave MAIL_HOST empty in production.
        if (mailHost.isBlank()) {
            log.warn("[DEV] MAIL_HOST chưa cấu hình, không gửi email. OTP đăng ký cho {}: {}", email, otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Mã xác thực đăng ký tài khoản Chat");
        message.setText("""
                Xin chào %s,

                Mã OTP đăng ký tài khoản của bạn là: %s

                Mã có hiệu lực trong %d phút. Không chia sẻ mã này cho bất kỳ ai.
                Nếu bạn không yêu cầu đăng ký, hãy bỏ qua email này.
                """.formatted(fullName, otp, ttl.toMinutes()));

        mailSender.getObject().send(message);
    }
}
