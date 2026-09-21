package com.meetingplanner.domains.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger("SECURITY");

    public void createAccountSuccess(String username, String ip) {
        log.info("event=create_account_success username={} ip={}", sanitize(username), sanitize(ip));
    }

    public void createAccountFailure(String username, String ip) {
        log.warn("event=create_account_failure username={} ip={}", sanitize(username), sanitize(ip));
    }

    public void loginSuccess(String username, String ip) {
        log.info("event=login_success username={} ip={}", sanitize(username), sanitize(ip));
    }

    public void loginFailure(String username, String ip, long remainingAttempts) {
        log.warn("event=login_failure username={} ip={} remaining_attempts={}",
                sanitize(username), sanitize(ip), remainingAttempts);
    }

    public void loginRejectedNotVerified(String email, String ip) {
        log.warn("event=login_rejected_not_verified email={} ip={}", sanitize(email), sanitize(ip));
    }

    public void emailVerified(String email, long userId) {
        log.info("event=email_verified email={} user_id={}", sanitize(email), userId);
    }

    public void forgotPasswordRequested(String email, String ip) {
        log.info("event=password_reset_requested email={} ip={}", sanitize(email), sanitize(ip));
    }

    public void forgotPasswordNotRequested(String email, String ip) {
        log.info("event=password_reset_no_account email={} ip={}", sanitize(email), sanitize(ip));
    }

    public void passwordResetCompleted(String email, String ip) {
        log.info("event=password_reset_completed email={} ip={}", sanitize(email), sanitize(ip));
    }

    public void accountLocked(String username, int tier, long durationMinutes, String ip) {
        log.warn("event=account_locked username={} tier={} duration_minutes={} ip={}",
                sanitize(username), tier, durationMinutes, sanitize(ip));
    }

    public void lockedAccountRejected(String username, String ip) {
        log.warn("event=locked_account_rejected username={} ip={}", sanitize(username), sanitize(ip));
    }

    public void tokenRefreshSuccess(long userId, String ip) {
        log.info("event=token_refresh_success user_id={} ip={}", userId, sanitize(ip));
    }

    public void tokenRefreshRejected(String reason, String ip) {
        log.warn("event=token_refresh_rejected reason={} ip={}", reason, sanitize(ip));
    }

    public void refreshTokenReuseDetected(long userId, String ip) {
        log.error("event=refresh_token_reuse_detected user_id={} ip={}", userId, sanitize(ip));
    }

    public void rateLimited(String scope, String ip) {
        log.warn("event=rate_limited scope={} ip={}", scope, sanitize(ip));
    }

    public void sessionLoggedOut(long userId, String ip, boolean allSessions) {
        log.info("event=session_logout user_id={} ip={} all_sessions={}", userId, sanitize(ip), allSessions);
    }

    public void logoutRejected(String reason, String ip) {
        log.warn("event=logout_rejected reason={} ip={}", reason, sanitize(ip));
    }

    private String sanitize(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.replaceAll("[\\r\\n]", "_");
    }
}