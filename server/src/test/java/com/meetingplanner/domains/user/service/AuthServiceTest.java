package com.meetingplanner.domains.user.service;

import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserAvailabilityEntity;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserAvailabilityRepository;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
}
