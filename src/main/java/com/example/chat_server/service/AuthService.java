package com.example.chat_server.service;

import com.example.chat_server.dto.AuthResponse;
import com.example.chat_server.dto.LoginRequest;
import com.example.chat_server.exception.ApiException;
import com.example.chat_server.model.User;
import com.example.chat_server.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(AuthService::invalidCredentials);

        if (user.getPassword() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "Tài khoản này được tạo qua Google, vui lòng đăng nhập bằng Google");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentials();
        }
        return tokenService.issueTokens(user);
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Sai username hoặc mật khẩu");
    }
}
