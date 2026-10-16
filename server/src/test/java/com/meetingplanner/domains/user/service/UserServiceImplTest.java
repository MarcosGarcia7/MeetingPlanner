package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.dto.UserProfileResponse;
import com.meetingplanner.common.api.dto.UserProfileUpdateRequest;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountTokenService accountTokenService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private JtiStorageService jtiStorageService;

    @InjectMocks
    private UserServiceImpl service;

    private static UserEntity user(long id) {
        UserEntity user = new UserEntity("alice", "alice@example.com", "hash", "#3B82F6", false);
        user.setId(id);
        return user;
    }

    private static UserProfileUpdateRequest onlyTimezone(String timezone) {
        return new UserProfileUpdateRequest(null, null, null, timezone, null, null, null);
    }

    @Test
    void updateMeSetsTimezone() {
        UserEntity user = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = service.updateMe(1L, onlyTimezone("America/New_York"));

        assertEquals("America/New_York", user.getTimezone());
        assertEquals("America/New_York", response.timezone());
    }

    @Test
    void updateMeClearsTimezoneOnBlank() {
        UserEntity user = user(1L);
        user.setTimezone("America/New_York");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = service.updateMe(1L, onlyTimezone("  "));

        assertNull(user.getTimezone());
        assertNull(response.timezone());
    }

    @Test
    void updateMeLeavesTimezoneUntouchedWhenNull() {
        UserEntity user = user(1L);
        user.setTimezone("Europe/Paris");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.updateMe(1L, onlyTimezone(null));

        assertEquals("Europe/Paris", user.getTimezone());
    }

    @Test
    void updateMeRejectsInvalidTimezone() {
        UserEntity user = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateMe(1L, onlyTimezone("Not/AZone")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).save(any(UserEntity.class));
    }
}
