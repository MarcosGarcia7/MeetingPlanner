package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.MeetingTimes;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingParticipantEntity;
import com.meetingplanner.domains.meeting.repository.MeetingReminderSentRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.notification.model.NotificationPreferenceEntity;
import com.meetingplanner.domains.notification.repository.NotificationPreferenceRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class MeetingReminderWorker {

    private static final int DEFAULT_LEAD_MINUTES = 30;

    private final MeetingRepository meetingRepository;
    private final MeetingReminderSentRepository reminderSentRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationService notificationService;

    // Time zone for users without a profile zone, from DEFAULT_TIMEZONE.
    @Value("${app.default-timezone:UTC}")
    private String defaultTimezone;

    public MeetingReminderWorker(MeetingRepository meetingRepository,
                                 MeetingReminderSentRepository reminderSentRepository,
                                 NotificationPreferenceRepository preferenceRepository,
                                 NotificationService notificationService) {
        this.meetingRepository = meetingRepository;
        this.reminderSentRepository = reminderSentRepository;
        this.preferenceRepository = preferenceRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${app.reminder.delay-ms:60000}", initialDelay = 20000)
    @Transactional
    public void sendDueReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime horizon = now.plusHours(24);

        for (MeetingEntity meeting : meetingRepository.findByStartTimeBetween(now, horizon)) {
            for (UserEntity user : recipientsOf(meeting)) {
                long userId = user.getId();

                if (reminderSentRepository.existsByMeetingIdAndUserId(meeting.getId(), userId)) {
                    continue;
                }

                NotificationPreferenceEntity prefs = preferenceRepository.findByUserId(userId).orElse(null);
                int leadMinutes = prefs != null ? prefs.getReminderLeadMinutes() : DEFAULT_LEAD_MINUTES;
                long minutesUntilStart = Duration.between(now, meeting.getStartTime()).toMinutes();
                if (minutesUntilStart < 0 || minutesUntilStart > leadMinutes) {
                    continue;
                }

                if (reminderSentRepository.claim(meeting.getId(), userId, now) == 0) {
                    continue;
                }

                notificationService.push(userId, "MEETING_REMINDER", "meeting_reminders",
                        "MEETING_REMINDER", reminderVars(meeting, user));
            }
        }
    }

    private Set<UserEntity> recipientsOf(MeetingEntity meeting) {
        Set<UserEntity> users = new LinkedHashSet<>();
        for (MeetingParticipantEntity participant : meeting.getParticipants()) {
            users.add(participant.getUser());
        }
        if (meeting.getCreator() != null) {
            users.add(meeting.getCreator());
        }
        return users;
    }

    private java.util.Map<String, String> reminderVars(MeetingEntity meeting, UserEntity user) {
        // Start only: the template reads "starts at {{start_time}}".
        String startTime = MeetingTimes.format(meeting.getStartTime(), null,
                MeetingTimes.zoneFor(user.getTimezone(), defaultTimezone));
        String name = user.getFirstName() != null && !user.getFirstName().isBlank()
                ? user.getFirstName()
                : user.getUsername();
        return java.util.Map.of(
                "name", name,
                "title", meeting.getTitle(),
                "start_time", startTime);
    }
}