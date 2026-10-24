package com.meetingplanner.it;

import com.meetingplanner.domains.meeting.service.MeetingReminderWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingReminderConcurrencyIT extends AbstractPostgresIT {

    private static final int ROUNDS = 5;

    @Autowired
    private MeetingReminderWorker reminderWorker;

    @Test
    void overlappingWorkerRunsSendEachReminderOnce() throws Exception {
        long alice = insertUser("alice");
        long bob = insertUser("bob");

        for (int round = 0; round < ROUNDS; round++) {
            long meetingId = insertMeeting(null, alice, OffsetDateTime.now().plusMinutes(10), bob);

            List<Outcome<Object>> outcomes = runConcurrently(
                    () -> { reminderWorker.sendDueReminders(); return null; },
                    () -> { reminderWorker.sendDueReminders(); return null; });

            for (Outcome<Object> outcome : outcomes) {
                assertTrue(outcome.succeeded(), () -> "worker failed: " + outcome.error());
            }
            assertEquals(2, count("select count(*) from meeting_reminders_sent where meeting_id = ?", meetingId));
        }
        // One reminder per user per meeting, never duplicated by the overlapping run.
        assertEquals(ROUNDS, count("select count(*) from notifications where user_id = ? and type = 'MEETING_REMINDER'", alice));
        assertEquals(ROUNDS, count("select count(*) from notifications where user_id = ? and type = 'MEETING_REMINDER'", bob));
    }
}
