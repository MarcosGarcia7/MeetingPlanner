package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.user.model.UserAvailabilityEntity;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserAvailabilityRepository;
import com.meetingplanner.domains.user.repository.UserRepository;
import com.meetingplanner.domains.user.service.UserProfileAuthorizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MeetingConflictService {

    private static final int MINUTES_PER_DAY = 24 * 60;

    private final MeetingRepository meetingRepository;
    private final UserAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final UserProfileAuthorizationService scheduleAuthorization;

    @Value("${app.default-timezone:UTC}")
    private String defaultTimezone;

    public MeetingConflictService(MeetingRepository meetingRepository,
                                  UserAvailabilityRepository availabilityRepository,
                                  UserRepository userRepository,
                                  UserProfileAuthorizationService scheduleAuthorization) {
        this.meetingRepository = meetingRepository;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.scheduleAuthorization = scheduleAuthorization;
    }

    @Transactional(readOnly = true)
    public List<MeetingEntity> findScheduleConflicts(long userId, OffsetDateTime start, OffsetDateTime end, Long excludeMeetingId) {
        if (start == null) {
            return List.of();
        }
        return meetingRepository.findScheduleConflicts(userId, start, effectiveEnd(start, end), excludeMeetingId);
    }

    @Transactional(readOnly = true)
    public List<String> scheduleWarningsForViewer(long viewerId, long targetUserId, OffsetDateTime start, OffsetDateTime end) {
        if (start == null || !scheduleAuthorization.canViewSchedule(viewerId, targetUserId)) {
            return List.of();
        }
        UserEntity target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) {
            return List.of();
        }
        List<MeetingEntity> conflicts = meetingRepository.findScheduleConflictsForUser(
                targetUserId, true, start, effectiveEnd(start, end), null);
        List<String> warnings = new ArrayList<>();
        for (MeetingEntity conflict : conflicts) {
            warnings.add(target.getUsername() + " may be busy: \"" + title(conflict) + "\" at "
                    + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(conflict.getStartTime()));
        }
        return warnings;
    }

    @Transactional(readOnly = true)
    public List<String> availabilityWarningsForViewer(long viewerId, long targetUserId, OffsetDateTime start, OffsetDateTime end) {
        if (!scheduleAuthorization.canViewSchedule(viewerId, targetUserId)) {
            return List.of();
        }
        return availabilityWarnings(targetUserId, start, end);
    }

    @Transactional(readOnly = true)
    public List<String> availabilityWarnings(long userId, OffsetDateTime start, OffsetDateTime end) {
        if (start == null) {
            return List.of();
        }
        UserAvailabilityEntity availability = availabilityRepository.findById(userId).orElse(null);
        if (availability == null || !availability.isAvailabilitySet()) {
            return List.of();
        }
        UserEntity user = userRepository.findById(userId).orElse(null);
        ZoneId zone = zoneFor(user);

        OffsetDateTime effectiveEnd = effectiveEnd(start, end);
        ZonedDateTime rangeStart = start.atZoneSameInstant(zone);
        ZonedDateTime rangeEnd = effectiveEnd.atZoneSameInstant(zone);

        for (LocalDate day = rangeStart.toLocalDate(); !day.isAfter(rangeEnd.toLocalDate()); day = day.plusDays(1)) {
            ZonedDateTime dayStart = day.atStartOfDay(zone);
            ZonedDateTime dayEnd = dayStart.plusDays(1);
            ZonedDateTime segmentStart = rangeStart.isAfter(dayStart) ? rangeStart : dayStart;
            ZonedDateTime segmentEnd = rangeEnd.isBefore(dayEnd) ? rangeEnd : dayEnd;
            if (!segmentStart.isBefore(segmentEnd)) {
                continue;
            }
            int startMinute = minuteOfDay(dayStart, segmentStart);
            int endMinute = minuteOfDay(dayStart, segmentEnd);
            if (!coveredByAvailability(availability, day.getDayOfWeek(), startMinute, endMinute)) {
                String who = user != null ? user.getUsername() : "this user";
                return List.of("Outside " + who + "'s stated availability");
            }
        }
        return List.of();
    }

    private boolean coveredByAvailability(UserAvailabilityEntity availability, DayOfWeek day, int startMinute, int endMinute) {
        int[][] windows = windowsFor(availability, day);
        if (windows == null || windows.length == 0) {
            return false;
        }
        List<int[]> sorted = new ArrayList<>();
        for (int[] window : windows) {
            if (window != null && window.length >= 2 && window[1] > window[0]) {
                sorted.add(new int[]{window[0], window[1]});
            }
        }
        if (sorted.isEmpty()) {
            return false;
        }
        sorted.sort(Comparator.comparingInt(w -> w[0]));

        int cursor = startMinute;
        for (int[] window : sorted) {
            if (window[1] <= cursor || window[0] > cursor) {
                continue;
            }
            cursor = Math.max(cursor, window[1]);
            if (cursor >= endMinute) {
                return true;
            }
        }
        return false;
    }

    private static int minuteOfDay(ZonedDateTime dayStart, ZonedDateTime moment) {
        long minutes = Duration.between(dayStart, moment).toMinutes();
        return (int) Math.min(MINUTES_PER_DAY, Math.max(0, minutes));
    }

    private ZoneId zoneFor(UserEntity user) {
        if (user != null && user.getTimezone() != null && !user.getTimezone().isBlank()) {
            try {
                return ZoneId.of(user.getTimezone());
            } catch (RuntimeException ignored) {
                // fall through to default
            }
        }
        return ZoneId.of(defaultTimezone);
    }

    private static int[][] windowsFor(UserAvailabilityEntity availability, DayOfWeek day) {
        return switch (day) {
            case MONDAY -> availability.getMonAvailability();
            case TUESDAY -> availability.getTueAvailability();
            case WEDNESDAY -> availability.getWedAvailability();
            case THURSDAY -> availability.getThuAvailability();
            case FRIDAY -> availability.getFriAvailability();
            case SATURDAY -> availability.getSatAvailability();
            case SUNDAY -> availability.getSunAvailability();
        };
    }

    private static OffsetDateTime effectiveEnd(OffsetDateTime start, OffsetDateTime end) {
        return end != null ? end : start;
    }

    private static String title(MeetingEntity meeting) {
        return meeting.getDescription() != null && !meeting.getDescription().isBlank()
                ? meeting.getDescription()
                : "Meeting";
    }
}
