package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserAvailabilityRepository;
import com.meetingplanner.domains.user.repository.UserRepository;
import com.meetingplanner.domains.user.service.UserProfileAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingConflictServiceTest {

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private UserAvailabilityRepository availabilityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileAuthorizationService scheduleAuthorization;

    @InjectMocks
    private MeetingConflictService service;

    private static UserEntity user(long id, String username, String timezone) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setTimezone(timezone);
        return user;
    }

    @Test
    void scheduleWarningShowsConflictTimeInViewersZone() {
        UserEntity alice = user(1L, "alice", "America/Chicago");
        UserEntity bob = user(2L, "bob", "Europe/Berlin");
        MeetingEntity busy = new MeetingEntity();
        busy.setTitle("Standup");
        busy.setStartTime(OffsetDateTime.parse("2026-12-01T15:00:00Z"));
        busy.setEndTime(OffsetDateTime.parse("2026-12-01T15:30:00Z"));
        OffsetDateTime start = OffsetDateTime.parse("2026-12-01T15:00:00Z");

        when(scheduleAuthorization.canViewSchedule(1L, 2L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(meetingRepository.findScheduleConflictsForUser(eq(2L), eq(true), eq(start), any(), isNull()))
                .thenReturn(List.of(busy));

        List<String> warnings = service.scheduleWarningsForViewer(1L, 2L, start, null);

        assertEquals(List.of("bob may be busy: \"Standup\" at Tue, Dec 1, 2026, 9:00 – 9:30 AM CST"), warnings);
    }

    @Test
    void scheduleWarningUsesConfiguredDefaultZoneWhenViewerHasNone() {
        ReflectionTestUtils.setField(service, "defaultTimezone", "America/Chicago");
        UserEntity alice = user(1L, "alice", null);
        UserEntity bob = user(2L, "bob", null);
        MeetingEntity busy = new MeetingEntity();
        busy.setTitle("Standup");
        busy.setStartTime(OffsetDateTime.parse("2026-12-01T15:00:00Z"));
        OffsetDateTime start = OffsetDateTime.parse("2026-12-01T15:00:00Z");

        when(scheduleAuthorization.canViewSchedule(1L, 2L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(meetingRepository.findScheduleConflictsForUser(eq(2L), eq(true), eq(start), any(), isNull()))
                .thenReturn(List.of(busy));

        assertEquals(List.of("bob may be busy: \"Standup\" at Tue, Dec 1, 2026, 9:00 AM CST"),
                service.scheduleWarningsForViewer(1L, 2L, start, null));
    }

    @Test
    void scheduleWarningsAreEmptyWhenViewerCannotSeeSchedule() {
        OffsetDateTime start = OffsetDateTime.parse("2026-12-01T15:00:00Z");
        when(scheduleAuthorization.canViewSchedule(1L, 2L)).thenReturn(false);

        assertTrue(service.scheduleWarningsForViewer(1L, 2L, start, null).isEmpty());
        verifyNoInteractions(meetingRepository, userRepository);
    }
}
