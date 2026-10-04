package com.example.chat_server.controller;

import com.example.chat_server.dto.*;
import com.example.chat_server.service.AuthService;
import com.example.chat_server.service.GoogleAuthService;
import com.example.chat_server.service.PasswordResetService;
import com.example.chat_server.service.RegistrationService;
import com.example.chat_server.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${app.api.prefix}/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final TokenService tokenService;
    private final PasswordResetService passwordResetService;

    public AuthController(RegistrationService registrationService,
                          AuthService authService,
                          GoogleAuthService googleAuthService,
                          TokenService tokenService,
                          PasswordResetService passwordResetService) {
        this.registrationService = registrationService;
        this.authService = authService;
        this.googleAuthService = googleAuthService;
        this.tokenService = tokenService;
        this.passwordResetService = passwordResetService;
    }

    // Quên mật khẩu bước 1: gửi OTP tới email (luôn 202, kể cả khi email không tồn tại)
    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.requestReset(request.email());
    }

    // Quên mật khẩu bước 2: xác thực OTP và đặt mật khẩu mới, đăng xuất mọi thiết bị
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }

    // Bước 1: gửi thông tin đăng ký, server gửi OTP tới email
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request);
    }

    @PostMapping("/register/resend-otp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RegisterResponse resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return registrationService.resendOtp(request.email());
    }

    // Bước 2: xác thực OTP -> tạo tài khoản và đăng nhập luôn
    @PostMapping("/register/verify")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return registrationService.verifyOtp(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return googleAuthService.login(request.idToken());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return tokenService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        tokenService.revoke(request.refreshToken());
    }
}
