package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.dto.AccountCreatedResponse;
import com.meetingplanner.common.api.dto.TokenResponse;
import com.meetingplanner.common.api.dto.UserProfileResponse;
import com.meetingplanner.common.api.exceptions.AccountLockedException;
import com.meetingplanner.common.api.exceptions.EmailNotVerifiedException;
import com.meetingplanner.common.api.exceptions.EmailTakenException;
import com.meetingplanner.common.api.exceptions.InvalidCredentialsException;
import com.meetingplanner.common.api.exceptions.RateLimitExceededException;
import com.meetingplanner.common.api.exceptions.RefreshTokenReuseException;
import com.meetingplanner.common.api.exceptions.UsernameTakenException;
import com.meetingplanner.common.api.model.LockoutBucket;
import com.meetingplanner.common.api.model.TokenPair;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.AccountTokenEntity;
import com.meetingplanner.domains.user.model.AccountTokenType;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Service
public class AuthService {

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       @Qualifier("refreshJwtDecoder") JwtDecoder refreshJwtDecoder,
                       LoginAttemptService loginAttemptService, AccountLockoutService accountLockoutService,
                       JtiStorageService jtiStorageService, SecurityAuditService auditService,
                       AccountTokenService accountTokenService, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshJwtDecoder = refreshJwtDecoder;
        this.loginAttemptService = loginAttemptService;
        this.accountLockoutService = accountLockoutService;
        this.jtiStorageService = jtiStorageService;
        this.auditService = auditService;
        this.accountTokenService = accountTokenService;
        this.notificationService = notificationService;
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
    private static final String FORGOT_RATE_LIMIT_PREFIX = "rl:forgot:";

    private static final int CREATE_MAX_PER_IP = 5;
    private static final long CREATE_WINDOW_SECONDS = 10 * 60;
    private static final int FORGOT_MAX_PER_IP = 5;
    private static final long FORGOT_WINDOW_SECONDS = 10 * 60;

    private static final String VERIFY_RESEND_PREFIX = "rl:verify-resend:";
    private static final int VERIFY_RESEND_MAX_PER_IP = 5;
    private static final long VERIFY_RESEND_WINDOW_SECONDS = 10 * 60;
    private static final int VERIFY_RESEND_MAX_PER_EMAIL = 3;
    private static final long VERIFY_RESEND_EMAIL_WINDOW_SECONDS = 10 * 60;

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final int USERNAME_MAX_LENGTH = 20;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,20}$");
    private static final Pattern FALLBACK_CLEAN = Pattern.compile("[^A-Za-z0-9_-]");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtDecoder refreshJwtDecoder;
    private final LoginAttemptService loginAttemptService;
    private final AccountLockoutService accountLockoutService;
    private final JtiStorageService jtiStorageService;
    private final SecurityAuditService auditService;
    private final AccountTokenService accountTokenService;
    private final NotificationService notificationService;

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    public AccountCreatedResponse create(String email, String password, String username, String firstName, String lastName,
                                         boolean isPrivate, String clientIp) {
        rateLimit(CREATE_RATE_LIMIT_PREFIX, clientIp, CREATE_MAX_PER_IP, CREATE_WINDOW_SECONDS, "create");

        String normalizedEmail = normalizeEmail(email);
        if (password == null || password.isBlank()) {
            throw new InvalidCredentialsException();
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            auditService.createAccountFailure(normalizedEmail, clientIp);
            throw new EmailTakenException(normalizedEmail);
        }

        String resolvedUsername = resolveUsername(normalizedEmail, username);

        UserEntity user = new UserEntity(resolvedUsername, normalizedEmail, passwordEncoder.encode(password), isPrivate);

        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            user.setLastName(lastName.trim());
        }

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            auditService.createAccountFailure(normalizedEmail, clientIp);
            throw new EmailTakenException(normalizedEmail);
        }

        auditService.createAccountSuccess(normalizedEmail, clientIp);

        String verifyToken = accountTokenService.create(user, AccountTokenType.EMAIL_VERIFY, null, clientIp, null);
        String verifyLink = appBaseUrl + "/verify-email?token=" + verifyToken;
        notificationService.queueSecurityEmail(user, "EMAIL_VERIFY", Map.of(
                "name", displayName(user),
                "link", verifyLink));

        return new AccountCreatedResponse(normalizedEmail,
                "Account created. A verification email has been sent to " + normalizedEmail + ".");
    }

    public TokenResponse loginByEmail(String email, String password, String clientIp) {
        String identifier = normalizeEmail(email);
        return authenticate(identifier, password, clientIp, () -> userRepository.findByEmail(identifier));
    }

    public TokenResponse loginByUsername(String username, String password, String clientIp) {
        if (username == null || username.isBlank()) {
            throw new InvalidCredentialsException();
        }
        String identifier = username.trim();
        return authenticate(identifier, password, clientIp, () -> userRepository.findByUsername(identifier));
    }

    private TokenResponse authenticate(String identifier, String password, String clientIp,
                                       Supplier<Optional<UserEntity>> resolver) {
        if (password == null || password.isBlank()) {
            throw new InvalidCredentialsException();
        }

        enforceNotLocked(identifier, clientIp);

        UserEntity user = resolver.get().orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            if (user == null) {
                passwordEncoder.matches(password, DUMMY_BCRYPT_HASH);
            }
            handleFailedLogin(identifier, clientIp);
        }

        if (user.getEmailVerifiedAt() == null) {
            auditService.loginRejectedNotVerified(identifier, clientIp);
            throw new EmailNotVerifiedException();
        }

        loginAttemptService.resetAttempts(IP_PREFIX + clientIp);
        auditService.loginSuccess(identifier, clientIp);

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

    public void verifyEmail(String rawToken) {
        AccountTokenEntity token = accountTokenService.consume(
                rawToken, AccountTokenType.EMAIL_VERIFY, AccountTokenType.EMAIL_CHANGE);

        UserEntity user = token.getUser();
        if (token.getTokenType() == AccountTokenType.EMAIL_CHANGE) {
            if (token.getEmail() != null && !token.getEmail().isBlank()) {
                user.setEmail(token.getEmail());
            } else if (user.getPendingEmail() != null) {
                user.setEmail(user.getPendingEmail());
            }
            user.setPendingEmail(null);
        }
        user.setEmailVerifiedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
        auditService.emailVerified(user.getEmail(), user.getId());
    }

    public void resendVerificationEmail(String email, String clientIp) {
        String normalizedEmail = normalizeEmail(email);
        rateLimit(VERIFY_RESEND_PREFIX + "ip:", clientIp, VERIFY_RESEND_MAX_PER_IP, VERIFY_RESEND_WINDOW_SECONDS, "verify-resend");
        rateLimit(VERIFY_RESEND_PREFIX, normalizedEmail, VERIFY_RESEND_MAX_PER_EMAIL, VERIFY_RESEND_EMAIL_WINDOW_SECONDS, "verify-resend");

        UserEntity user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || user.getEmailVerifiedAt() != null) {
            return;
        }

        String verifyToken = accountTokenService.create(user, AccountTokenType.EMAIL_VERIFY, null, clientIp, null);
        String verifyLink = appBaseUrl + "/verify-email?token=" + verifyToken;
        notificationService.queueSecurityEmail(user, "EMAIL_VERIFY", Map.of(
                "name", displayName(user),
                "link", verifyLink));
    }

    public void forgotPassword(String email, String clientIp) {
        rateLimit(FORGOT_RATE_LIMIT_PREFIX, clientIp, FORGOT_MAX_PER_IP, FORGOT_WINDOW_SECONDS, "forgot_password");

        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            auditService.forgotPasswordNotRequested(normalizedEmail, clientIp);
            return;
        }

        String resetToken = accountTokenService.create(user, AccountTokenType.PASSWORD_RESET, null, clientIp, null);
        String resetLink = appBaseUrl + "/reset-password?token=" + resetToken;
        notificationService.queueSecurityEmail(user, "PASSWORD_RESET", Map.of(
                "name", displayName(user),
                "link", resetLink));
        auditService.forgotPasswordRequested(normalizedEmail, clientIp);
    }

    public void resetPassword(String rawToken, String newPassword, String clientIp) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new InvalidCredentialsException();
        }

        AccountTokenEntity token = accountTokenService.consume(rawToken, AccountTokenType.PASSWORD_RESET);
        UserEntity user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(OffsetDateTime.now());
        accountTokenService.deleteForUser(user.getId(), AccountTokenType.PASSWORD_RESET);
        jtiStorageService.revokeAllJtis(user.getId().toString());
        userRepository.save(user);
        auditService.passwordResetCompleted(user.getEmail(), clientIp);
    }

    private TokenResponse issueTokens(UserEntity user) {
        loginAttemptService.resetAttempts(user.getUsername());

        TokenPair pair = tokenService.generateTokenPair(user.getId().toString(), user.getUsername());
        jtiStorageService.addValidJti(user.getId().toString(), pair.refreshJti());

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isPrivate(),
                user.isFriendsCanViewPrivate(),
                user.getEmailVerifiedAt() != null
        );

        return new TokenResponse(pair.accessToken(), pair.refreshToken(), profile);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveUsername(String email, String provided) {
        if (provided != null && !provided.isBlank()) {
            String candidate = provided.trim();
            if (candidate.length() > USERNAME_MAX_LENGTH || !USERNAME_PATTERN.matcher(candidate).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Username may only contain letters, digits, hyphens, and underscores, up to "
                                + USERNAME_MAX_LENGTH + " characters");
            }
            if (userRepository.existsByUsername(candidate)) {
                throw new UsernameTakenException(candidate);
            }
            return candidate;
        }

        String fallback = sanitizeFallback(emailLocalPart(email));
        if (fallback.isEmpty()) {
            fallback = "user";
        }
        String base = fallback;
        int suffix = 1;
        String candidate = fallback;
        while (userRepository.existsByUsername(candidate)) {
            String stem = base.substring(0, Math.min(base.length(), 14));
            candidate = stem + "-" + suffix++;
            if (candidate.length() > USERNAME_MAX_LENGTH) {
                candidate = candidate.substring(0, USERNAME_MAX_LENGTH);
            }
        }
        return candidate;
    }

    private String sanitizeFallback(String raw) {
        String cleaned = FALLBACK_CLEAN.matcher(raw).replaceAll("");
        if (cleaned.length() > USERNAME_MAX_LENGTH) {
            cleaned = cleaned.substring(0, USERNAME_MAX_LENGTH);
        }
        return cleaned;
    }

    private String emailLocalPart(String email) {
        String local = email;
        int at = email.indexOf('@');
        if (at > 0) {
            local = email.substring(0, at);
        }
        return local;
    }

    private String displayName(UserEntity user) {
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return user.getFirstName();
        }
        return user.getUsername();
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