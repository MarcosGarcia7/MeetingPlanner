package com.meetingplanner.common.api.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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

    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleIncorrectPassword(IncorrectPasswordException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "incorrect_password",
                "message", e.getMessage()
        ));
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

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "forbidden",
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

    @ExceptionHandler(GroupNameTakenException.class)
    public ResponseEntity<Map<String, Object>> handleGroupNameTaken(GroupNameTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "group_name_taken",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String message = e.getReason() != null
                ? e.getReason()
                : status != null ? status.getReasonPhrase() : "Request failed";
        String error = status != null ? errorCode(status) : "http_" + e.getStatusCode().value();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "error", error,
                "message", message
        ));
    }

    private static String errorCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "bad_request";
            case UNAUTHORIZED -> "unauthorized";
            case FORBIDDEN -> "forbidden";
            case NOT_FOUND -> "not_found";
            case METHOD_NOT_ALLOWED -> "method_not_allowed";
            case CONFLICT -> "conflict";
            case GONE -> "gone";
            case UNSUPPORTED_MEDIA_TYPE -> "unsupported_media_type";
            case TOO_MANY_REQUESTS -> "rate_limited";
            default -> "http_" + status.value();
        };
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedBody(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "malformed_request",
                "message", "Request body is malformed"
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "invalid_parameter",
                "message", "'" + e.getName() + "' has an invalid value"
        ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of(
                "error", "method_not_allowed",
                "message", "Method " + e.getMethod() + " is not supported"
        ));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of(
                "error", "unsupported_media_type",
                "message", "Unsupported content type"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "internal_error",
                "message", "Something went wrong"
        ));
    }
}