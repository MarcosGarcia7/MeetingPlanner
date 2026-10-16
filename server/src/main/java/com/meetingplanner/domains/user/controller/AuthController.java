package com.meetingplanner.domains.user.controller;

import com.meetingplanner.common.api.dto.*;
import com.meetingplanner.domains.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/create")
    public ResponseEntity<AccountCreatedResponse> create(@Valid @RequestBody CreateAccountRequest request,
                                                         HttpServletRequest httpRequest) {
        AccountCreatedResponse response = authService.create(request.email(), request.password(), request.username(),
                request.firstName(), request.lastName(), request.isPrivate(), request.timezone(),
                resolveClientIp(httpRequest));
        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/login/email")
    public TokenResponse loginEmail(@Valid @RequestBody LoginEmailRequest request, HttpServletRequest httpRequest) {
        return authService.loginByEmail(request.email(), request.password(), resolveClientIp(httpRequest));
    }

    @PostMapping("/login/username")
    public TokenResponse loginUsername(@Valid @RequestBody LoginUsernameRequest request,
                                       HttpServletRequest httpRequest) {
        return authService.loginByUsername(request.username(), request.password(), resolveClientIp(httpRequest));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request.refreshToken(), resolveClientIp(httpRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
        authService.logout(request.refreshToken(), request.allSessions(), resolveClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email/resend")
    public ResponseEntity<Void> resendVerificationEmail(@Valid @RequestBody VerifyEmailResendRequest request,
                                                        HttpServletRequest httpRequest) {
        authService.resendVerificationEmail(request.email(), resolveClientIp(httpRequest));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                               HttpServletRequest httpRequest) {
        authService.forgotPassword(request.email(), resolveClientIp(httpRequest));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                              HttpServletRequest httpRequest) {
        authService.resetPassword(request.token(), request.newPassword(), resolveClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}