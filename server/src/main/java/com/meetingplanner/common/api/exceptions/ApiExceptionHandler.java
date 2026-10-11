package com.meetingplanner.common.api.exceptions;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(AccountLockedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "account_locked",
                "message", e.getMessage(),
                "retryAfterSeconds", e.getSecondsLeft()
        ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "invalid_credentials");
        body.put("message", e.getRemainingAttempts() != null
                ? "Invalid credentials. You have " + e.getRemainingAttempts() + " attempts remaining."
                : "Invalid credentials.");
        if (e.getRemainingAttempts() != null) {
            body.put("remainingAttempts", e.getRemainingAttempts());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(RefreshTokenReuseException.class)
    public ResponseEntity<Map<String, Object>> handleRefreshTokenReuse(RefreshTokenReuseException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "refresh_token_reuse",
                "message", "Refresh token reuse detected. All sessions have been revoked."
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "validation_failed",
                "message", message
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "not_found",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(UsernameTakenException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameTaken(UsernameTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "username_taken",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(EmailTakenException.class)
    public ResponseEntity<Map<String, Object>> handleEmailTaken(EmailTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "email_taken",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "email_not_verified",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "invalid_token",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(Map.of(
                        "error", "rate_limited",
                        "message", e.getMessage(),
                        "retryAfterSeconds", e.getRetryAfterSeconds()
                ));
    }

    @ExceptionHandler(AvailabilityNotSetException.class)
    public ResponseEntity<Map<String, Object>> handleAvailabilityNotSet(AvailabilityNotSetException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "not_found",
                "message", e.getMessage()
        ));
    }
}