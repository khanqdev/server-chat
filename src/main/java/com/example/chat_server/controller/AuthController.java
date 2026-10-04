package com.example.chat_server.controller;

import com.example.chat_server.dto.*;
import com.example.chat_server.security.RefreshCookie;
import com.example.chat_server.service.AuthService;
import com.example.chat_server.service.GoogleAuthService;
import com.example.chat_server.service.PasswordResetService;
import com.example.chat_server.service.RegistrationService;
import com.example.chat_server.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
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
    private final RefreshCookie refreshCookie;

    public AuthController(RegistrationService registrationService,
                          AuthService authService,
                          GoogleAuthService googleAuthService,
                          TokenService tokenService,
                          PasswordResetService passwordResetService,
                          RefreshCookie refreshCookie) {
        this.registrationService = registrationService;
        this.authService = authService;
        this.googleAuthService = googleAuthService;
        this.tokenService = tokenService;
        this.passwordResetService = passwordResetService;
        this.refreshCookie = refreshCookie;
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
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpServletResponse response) {
        return withRefreshCookie(registrationService.verifyOtp(request), response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return withRefreshCookie(authService.login(request), response);
    }

    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request, HttpServletResponse response) {
        return withRefreshCookie(googleAuthService.login(request.idToken()), response);
    }

    // Không có body: refresh token nằm trong cookie httpOnly và được xoay vòng mỗi lần gọi
    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken,
                                HttpServletResponse response) {
        return withRefreshCookie(tokenService.refresh(refreshToken), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken,
                       HttpServletResponse response) {
        tokenService.revoke(refreshToken);
        refreshCookie.clear(response);
    }

    // Quên mật khẩu bước 1: gửi OTP tới email (luôn 202, kể cả khi email không tồn tại)
    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RegisterResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.requestReset(request.email());
    }

    // Quên mật khẩu bước 2: xác thực OTP và đặt mật khẩu mới, đăng xuất mọi thiết bị
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }

    private AuthResponse withRefreshCookie(TokenService.IssuedTokens tokens, HttpServletResponse response) {
        refreshCookie.set(response, tokens.refreshToken());
        return tokens.response();
    }
}
