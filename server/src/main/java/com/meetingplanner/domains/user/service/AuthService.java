package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.dto.TokenResponse;
import com.meetingplanner.common.api.exceptions.AccountLockedException;
import com.meetingplanner.common.api.exceptions.InvalidCredentialsException;
import com.meetingplanner.common.api.exceptions.RateLimitExceededException;
import com.meetingplanner.common.api.exceptions.RefreshTokenReuseException;
import com.meetingplanner.common.api.exceptions.UsernameTakenException;
import com.meetingplanner.common.api.model.LockoutBucket;
import com.meetingplanner.common.api.model.TokenPair;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       @Qualifier("refreshJwtDecoder") JwtDecoder refreshJwtDecoder,
                       LoginAttemptService loginAttemptService, AccountLockoutService accountLockoutService,
                       JtiStorageService jtiStorageService, SecurityAuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshJwtDecoder = refreshJwtDecoder;
        this.loginAttemptService = loginAttemptService;
        this.accountLockoutService = accountLockoutService;
        this.jtiStorageService = jtiStorageService;
        this.auditService = auditService;
    }

    private static final int USER_MAX_ATTEMPTS = 5;
    private static final long USER_LOCKOUT_BASE_MINUTES = 15;
    private static final long MAX_LOCKOUT_MINUTES = 24 * 60;
    private static final int IP_LOGIN_MAX_ATTEMPTS = 20;
    private static final long IP_LOGIN_WINDOW_SECONDS = 15 * 60;
    private static final int REFRESH_MAX_PER_IP = 60;
    private static final long REFRESH_WINDOW_SECONDS = 60;

    private static final String IP_PREFIX = "ip:";
    private static final String REFRESH_RATE_LIMIT_PREFIX = "rl:refresh:";
    private static final String CREATE_RATE_LIMIT_PREFIX = "rl:create:";

    private static final int CREATE_MAX_PER_IP = 5;
    private static final long CREATE_WINDOW_SECONDS = 10 * 60;

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtDecoder refreshJwtDecoder;
    private final LoginAttemptService loginAttemptService;
    private final AccountLockoutService accountLockoutService;
    private final JtiStorageService jtiStorageService;
    private final SecurityAuditService auditService;

    public TokenResponse create(String username, String password, String firstName, String lastName, boolean isPrivate, String clientIp) {
        rateLimit(CREATE_RATE_LIMIT_PREFIX, clientIp, CREATE_MAX_PER_IP, CREATE_WINDOW_SECONDS, "create");

        String normalizedUsername = normalizeUsername(username);
        if (password == null || password.isBlank()) {
            throw new InvalidCredentialsException();
        }

        if (userRepository.existsByUsername(normalizedUsername)) {
            auditService.createAccountFailure(normalizedUsername, clientIp);
            throw new UsernameTakenException(normalizedUsername);
        }

        UserEntity user = new UserEntity(normalizedUsername, passwordEncoder.encode(password), isPrivate);

        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            user.setLastName(lastName.trim());
        }

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            auditService.createAccountFailure(normalizedUsername, clientIp);
            throw new UsernameTakenException(normalizedUsername);
        }

        auditService.createAccountSuccess(normalizedUsername, clientIp);
        return issueTokens(user);
    }

    public TokenResponse login(String username, String password, String clientIp) {
        String normalizedUsername = normalizeUsername(username);
        if (password == null || password.isBlank()) {
            throw new InvalidCredentialsException();
        }

        enforceNotLocked(normalizedUsername, clientIp);

        UserEntity user = userRepository.findByUsername(normalizedUsername).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            if (user == null) {
                passwordEncoder.matches(password, DUMMY_BCRYPT_HASH);
            }
            handleFailedLogin(normalizedUsername, clientIp);
        }

        loginAttemptService.resetAttempts(IP_PREFIX + clientIp);
        auditService.loginSuccess(normalizedUsername, clientIp);

        return issueTokens(user);
    }

    public TokenResponse refresh(String refreshToken, String clientIp) {
        rateLimit(REFRESH_RATE_LIMIT_PREFIX, clientIp, REFRESH_MAX_PER_IP, REFRESH_WINDOW_SECONDS, "refresh");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
        }

        Jwt jwt;
        try {
            jwt = refreshJwtDecoder.decode(refreshToken);
        } catch (JwtException e) {
            auditService.tokenRefreshRejected("expired_or_invalid", clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Expired or invalid refresh token");
        }

        if (!"refresh".equals(jwt.getClaim("type"))) {
            auditService.tokenRefreshRejected("invalid_type", clientIp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token type");
        }

        String jti = jwt.getClaim("jti");
        if (jti == null || jti.isBlank()) {
            auditService.tokenRefreshRejected("missing_jti", clientIp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is missing its id");
        }

        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            auditService.tokenRefreshRejected("invalid_subject", clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token subject");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        boolean consumed = jtiStorageService.consumeJti(user.getId().toString(), jti);
        if (!consumed) {
            jtiStorageService.revokeAllJtis(user.getId().toString());
            auditService.refreshTokenReuseDetected(userId, clientIp);
            throw new RefreshTokenReuseException();
        }

        auditService.tokenRefreshSuccess(userId, clientIp);
        return issueTokens(user);
    }

    public void logout(String refreshToken, boolean allSessions, String clientIp) {
        if (refreshToken == null || refreshToken.isBlank()) {
            auditService.logoutRejected("missing_token", clientIp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
        }

        Jwt jwt;
        try {
            jwt = refreshJwtDecoder.decode(refreshToken);
        } catch (JwtException e) {
            auditService.logoutRejected("invalid_token", clientIp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid refresh token");
        }

        if (!"refresh".equals(jwt.getClaim("type"))) {
            auditService.logoutRejected("invalid_type", clientIp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token type");
        }

        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            auditService.logoutRejected("invalid_subject", clientIp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid refresh token subject");
        }

        if (allSessions) {
            jtiStorageService.revokeAllJtis(Long.toString(userId));
            auditService.sessionLoggedOut(userId, clientIp, true);
            return;
        }

        String jti = jwt.getClaim("jti");
        if (jti == null || jti.isBlank()) {
            auditService.logoutRejected("missing_jti", clientIp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is missing its id");
        }

        boolean consumed = jtiStorageService.consumeJti(Long.toString(userId), jti);
        if (!consumed) {
            auditService.logoutRejected("token_already_revoked", clientIp);
            return;
        }

        auditService.sessionLoggedOut(userId, clientIp, false);
    }

    private TokenResponse issueTokens(UserEntity user) {
        loginAttemptService.resetAttempts(user.getUsername());

        TokenPair pair = tokenService.generateTokenPair(user.getId().toString(), user.getUsername());
        jtiStorageService.addValidJti(user.getId().toString(), pair.refreshJti());

        return new TokenResponse(pair.accessToken(), pair.refreshToken());
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void handleFailedLogin(String username, String clientIp) {
        if (loginAttemptService.isBlocked(IP_PREFIX + clientIp, IP_LOGIN_MAX_ATTEMPTS)) {
            long retryAfter = loginAttemptService.getRemainingWindowSeconds(IP_PREFIX + clientIp);
            auditService.rateLimited("login", clientIp);
            throw new RateLimitExceededException(retryAfter > 0 ? retryAfter : IP_LOGIN_WINDOW_SECONDS);
        }

        loginAttemptService.incrementAttempts(IP_PREFIX + clientIp, IP_LOGIN_WINDOW_SECONDS);
        long attempts = loginAttemptService.incrementAttempts(username);

        if (loginAttemptService.isBlocked(username, USER_MAX_ATTEMPTS)) {
            LockoutBucket existing = accountLockoutService.getLockoutDetails(username);
            int tier = existing != null ? existing.getTier() + 1 : 1;
            long minutes = Math.min(USER_LOCKOUT_BASE_MINUTES * (1L << (tier - 1)), MAX_LOCKOUT_MINUTES);
            loginAttemptService.resetAttempts(username);
            accountLockoutService.lockAccount(username, tier, "Too many failed login attempts", minutes);
            auditService.accountLocked(username, tier, minutes, clientIp);
            throw new AccountLockedException("Too many failed login attempts", minutes * 60);
        }

        long remainingAttempts = (long) (USER_MAX_ATTEMPTS - attempts);
        auditService.loginFailure(username, clientIp, remainingAttempts);
        throw new InvalidCredentialsException(remainingAttempts);
    }

    private void rateLimit(String prefix, String key, int maxAttempts, long windowSeconds, String scope) {
        String targetKey = prefix + key;
        if (loginAttemptService.isBlocked(targetKey, maxAttempts)) {
            long retryAfter = loginAttemptService.getRemainingWindowSeconds(targetKey);
            auditService.rateLimited(scope, key);
            throw new RateLimitExceededException(retryAfter > 0 ? retryAfter : windowSeconds);
        }
        loginAttemptService.incrementAttempts(targetKey, windowSeconds);
    }

    private void enforceNotLocked(String username, String clientIp) {
        if (!accountLockoutService.isAccountLocked(username)) {
            return;
        }

        LockoutBucket details = accountLockoutService.getLockoutDetails(username);
        long secondsLeft = accountLockoutService.getRemainingLockoutTimeInSeconds(username);
        String reason = details != null ? details.getReason() : "Too many failed login attempts";
        auditService.lockedAccountRejected(username, clientIp);
        throw new AccountLockedException(reason, secondsLeft);
    }
}