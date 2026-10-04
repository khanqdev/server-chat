package com.example.chat_server.security;

import com.example.chat_server.exception.ErrorCode;
import com.example.chat_server.exception.GlobalExceptionHandler.ErrorBody;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Tiền tố chung của REST API, cấu hình bằng API_PREFIX trong .env
    @Value("${app.api.prefix}")
    private String apiPrefix;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Mã hóa mật khẩu an toàn
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Tắt CSRF vì dùng REST API / JWT; cookie refresh là SameSite=Strict
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(apiPrefix + "/auth/**", "/ws-chat/**", "/error").permitAll() // Cho phép đăng ký, đăng nhập và kết nối WS không cần token trước
                        .anyRequest().authenticated()
                )
                // Lỗi 401/403 cũng trả về đúng định dạng { error: { code, message } } của contract
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) -> writeError(response, objectMapper,
                                HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Authentication is required"))
                        .accessDeniedHandler((request, response, ex) -> writeError(response, objectMapper,
                                HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access is denied"))
                );

        return http.build();
    }

    private static void writeError(HttpServletResponse response, ObjectMapper objectMapper,
                                   HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ErrorBody.of(code, message, null));
    }
}
