package com.meetingplanner.it;

import com.meetingplanner.common.api.dto.NotificationPreferenceUpdateRequest;
import com.meetingplanner.common.api.dto.NotificationPreferencesResponse;
import com.meetingplanner.domains.notification.service.NotificationPreferenceService;
import com.meetingplanner.domains.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationPreferencesIT extends AbstractPostgresIT {

    private static final int ROUNDS = 10;

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private NotificationPreferenceService preferenceService;

    @Test
    void firstPushCreatesDefaultPreferencesAndStoresNotification() {
        long bob = insertUser("bob");

        notificationService.push(bob, "MEETING_UPDATED", "meeting_updates", "MEETING_UPDATED", Map.of("title", "t"));

        assertEquals(1, count("select count(*) from notification_preferences where user_id = ?", bob));
        assertEquals(1, count("select count(*) from notifications where user_id = ?", bob));
        // email_enabled defaults to true, so the email is queued too.
        assertEquals(1, count("select count(*) from email_outbox where recipient_user_id = ?", bob));
    }

    @Test
    void concurrentFirstPushesForSameUserBothSucceed() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            long user = insertUser("user" + round);

            List<Outcome<Object>> outcomes = runConcurrently(
                    () -> { notificationService.push(user, "X", "meeting_updates", "MEETING_UPDATED", Map.of()); return null; },
                    () -> { notificationService.push(user, "X", "meeting_updates", "MEETING_UPDATED", Map.of()); return null; });

            for (Outcome<Object> outcome : outcomes) {
                assertTrue(outcome.succeeded(), () -> "push failed: " + outcome.error());
            }
            assertEquals(1, count("select count(*) from notification_preferences where user_id = ?", user));
            assertEquals(2, count("select count(*) from notifications where user_id = ?", user));
        }
    }

    @Test
    void getAndUpdateWorkForUserWithoutPreferencesRow() {
        long alice = insertUser("alice");
        long bob = insertUser("bob");

        NotificationPreferencesResponse defaults = preferenceService.get(alice);
        assertTrue(defaults.emailEnabled());

        NotificationPreferencesResponse updated = preferenceService.update(bob,
                new NotificationPreferenceUpdateRequest(false, null, null, null, null, null, null, null, 60));
        assertFalse(updated.emailEnabled());
        assertEquals(1, count("select count(*) from notification_preferences where user_id = ? "
                + "and email_enabled = false and reminder_lead_minutes = 60", bob));
    }
}
