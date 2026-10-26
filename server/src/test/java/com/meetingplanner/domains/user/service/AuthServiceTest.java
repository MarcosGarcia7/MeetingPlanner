package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.dto.TokenResponse;
import com.meetingplanner.common.api.exceptions.IncorrectPasswordException;
import com.meetingplanner.common.api.exceptions.InvalidTokenException;
import com.meetingplanner.common.api.exceptions.RateLimitExceededException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.TokenPair;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.AccountTokenEntity;
import com.meetingplanner.domains.user.model.AccountTokenType;
import com.meetingplanner.domains.user.model.UserAvailabilityEntity;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserAvailabilityRepository;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private JwtDecoder refreshJwtDecoder;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private AccountLockoutService accountLockoutService;
    @Mock
    private JtiStorageService jtiStorageService;
    @Mock
    private SecurityAuditService auditService;
    @Mock
    private AccountTokenService accountTokenService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserAvailabilityRepository availabilityRepository;

    @InjectMocks
    private AuthService service;

    private void stubHappyPath() {
        when(loginAttemptService.isBlocked(anyString(), anyInt())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
    }

    @Test
    void createPersistsNormalizedTimezone() {
        stubHappyPath();

        service.create("a@example.com", "password123456", "alice", "Al", "Smith", false,
                "America/New_York", "1.2.3.4");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("America/New_York", userCaptor.getValue().getTimezone());
    }

    @Test
    void createSeedsEmptyAvailabilityRow() {
        stubHappyPath();

        service.create("a@example.com", "password123456", "alice", "Al", "Smith", false,
                "Europe/Paris", "1.2.3.4");

        ArgumentCaptor<UserAvailabilityEntity> captor = ArgumentCaptor.forClass(UserAvailabilityEntity.class);
        verify(availabilityRepository).save(captor.capture());
        UserAvailabilityEntity availability = captor.getValue();
        assertNotNull(availability.getUser());
        assertFalse(availability.isAvailabilitySet());
        assertEquals(0, availability.getMonAvailability().length);
        assertEquals(0, availability.getSunAvailability().length);
    }

    @Test
    void createWithoutTimezoneLeavesItNull() {
        stubHappyPath();

        service.create("a@example.com", "password123456", "alice", "Al", "Smith", false,
                null, "1.2.3.4");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(null, userCaptor.getValue().getTimezone());
    }

    @Test
    void createWithInvalidTimezoneIsRejectedBeforePersisting() {
        stubHappyPath();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create("a@example.com", "password123456", "alice", "Al", "Smith", false,
                        "Not/AZone", "1.2.3.4"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(UserEntity.class));
        verify(availabilityRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static UserEntity existingUser() {
        UserEntity user = new UserEntity("alice", "alice@example.com", "old-hash", "#3B82F6", false);
        user.setId(1L);
        return user;
    }

    @Test
    void changePasswordRevokesSessionsAndIssuesFreshTokens() {
        UserEntity user = existingUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-hash");
        when(tokenService.generateTokenPair("1", "alice")).thenReturn(new TokenPair("access", "refresh", "jti"));

        TokenResponse response = service.changePassword(1L, "current-password", "new-password-123", "1.2.3.4");

        assertEquals("new-hash", user.getPasswordHash());
        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        InOrder order = inOrder(jtiStorageService);
        order.verify(jtiStorageService).revokeAllJtis("1");
        order.verify(jtiStorageService).addValidJti("1", "jti");
        verify(accountTokenService).deleteForUser(1L, AccountTokenType.PASSWORD_RESET);
        verify(userRepository).save(user);
        verify(notificationService).queueSecurityEmail(eq(user), eq("PASSWORD_CHANGED"), anyMap());
    }

    @Test
    void changePasswordRejectsWrongCurrentPasswordWithoutChangingAnything() {
        UserEntity user = existingUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThrows(IncorrectPasswordException.class,
                () -> service.changePassword(1L, "wrong-password", "new-password-123", "1.2.3.4"));

        assertEquals("old-hash", user.getPasswordHash());
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(jtiStorageService, never()).revokeAllJtis(anyString());
        verify(accountLockoutService, never()).lockAccount(anyString(), anyInt(), anyString(), anyLong());
    }

    @Test
    void changePasswordForMissingUserIsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.changePassword(1L, "current-password", "new-password-123", "1.2.3.4"));

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changePasswordIsRateLimitedPerUser() {
        when(loginAttemptService.isBlocked("rl:change-password:1", 5)).thenReturn(true);
        when(loginAttemptService.getRemainingWindowSeconds("rl:change-password:1")).thenReturn(120L);

        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
                () -> service.changePassword(1L, "current-password", "new-password-123", "1.2.3.4"));

        assertEquals(120L, exception.getRetryAfterSeconds());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void forgotPasswordQueuesResetEmailWithOneHourToken() {
        UserEntity user = existingUser();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(accountTokenService.create(user, AccountTokenType.PASSWORD_RESET, null, "1.2.3.4", Duration.ofHours(1)))
                .thenReturn("raw-token");

        service.forgotPassword(" Alice@Example.com ", "1.2.3.4");

        verify(notificationService).queueSecurityEmail(eq(user), eq("PASSWORD_RESET"), anyMap());
    }

    @Test
    void forgotPasswordForUnknownEmailDoesNothing() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        service.forgotPassword("nobody@example.com", "1.2.3.4");

        verify(accountTokenService, never()).create(any(), any(), any(), any(), any());
        verify(notificationService, never()).queueSecurityEmail(any(), anyString(), anyMap());
    }

    @Test
    void forgotPasswordIsRateLimitedPerEmailBeforeLookup() {
        when(loginAttemptService.isBlocked("rl:forgot:1.2.3.4", 5)).thenReturn(false);
        when(loginAttemptService.isBlocked("rl:forgot:email:alice@example.com", 3)).thenReturn(true);
        when(loginAttemptService.getRemainingWindowSeconds("rl:forgot:email:alice@example.com")).thenReturn(300L);

        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
                () -> service.forgotPassword("alice@example.com", "1.2.3.4"));

        assertEquals(300L, exception.getRetryAfterSeconds());
        verify(userRepository, never()).findByEmail(anyString());
    }

    private AccountTokenEntity resetToken(UserEntity user) {
        AccountTokenEntity token = new AccountTokenEntity();
        token.setUser(user);
        token.setTokenType(AccountTokenType.PASSWORD_RESET);
        return token;
    }

    @Test
    void resetPasswordUpdatesHashRevokesSessionsAndClearsLockouts() {
        UserEntity user = existingUser();
        user.setEmailVerifiedAt(OffsetDateTime.now().minusDays(1));
        when(accountTokenService.consume("raw-token", AccountTokenType.PASSWORD_RESET)).thenReturn(resetToken(user));
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-hash");

        service.resetPassword("raw-token", "new-password-123", "1.2.3.4");

        assertEquals("new-hash", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(accountTokenService).deleteForUser(1L, AccountTokenType.PASSWORD_RESET);
        verify(jtiStorageService).revokeAllJtis("1");
        verify(accountLockoutService).unlockAccount("alice@example.com");
        verify(accountLockoutService).unlockAccount("alice");
        verify(loginAttemptService).resetAttempts("alice@example.com");
        verify(loginAttemptService).resetAttempts("alice");
        verify(notificationService).queueSecurityEmail(eq(user), eq("PASSWORD_CHANGED"), anyMap());
    }

    @Test
    void resetPasswordVerifiesUnverifiedEmail() {
        UserEntity user = existingUser();
        assertNull(user.getEmailVerifiedAt());
        when(accountTokenService.consume("raw-token", AccountTokenType.PASSWORD_RESET)).thenReturn(resetToken(user));
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-hash");

        service.resetPassword("raw-token", "new-password-123", "1.2.3.4");

        assertNotNull(user.getEmailVerifiedAt());
    }

    @Test
    void resetPasswordWithInvalidTokenChangesNothing() {
        when(accountTokenService.consume("bad-token", AccountTokenType.PASSWORD_RESET))
                .thenThrow(new InvalidTokenException("Invalid or expired token"));

        assertThrows(InvalidTokenException.class,
                () -> service.resetPassword("bad-token", "new-password-123", "1.2.3.4"));

        verify(userRepository, never()).save(any(UserEntity.class));
        verify(jtiStorageService, never()).revokeAllJtis(anyString());
        verify(notificationService, never()).queueSecurityEmail(any(), anyString(), anyMap());
    }
}
