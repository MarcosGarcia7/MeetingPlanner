package com.meetingplanner.it;

import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.domains.invite.service.InviteService;
import com.meetingplanner.domains.meeting.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingAcceptDeleteConcurrencyIT extends AbstractPostgresIT {

    private static final int ROUNDS = 10;

    @Autowired
    private InviteService inviteService;
    @Autowired
    private MeetingService meetingService;

    @Test
    void acceptRacingDeleteNeitherDeadlocksNorHitsForeignKey() throws Exception {
        long owner = insertUser("owner");
        long guest = insertUser("guest");
        long groupId = insertGroup("team", owner, false);

        for (int round = 0; round < ROUNDS; round++) {
            long meetingId = insertMeeting(groupId, owner, OffsetDateTime.now().plusDays(2).plusHours(round));
            long inviteId = insertPendingMeetingInvite(meetingId, owner, guest, "guest@example.com");

            List<Outcome<Object>> outcomes = runConcurrently(
                    () -> inviteService.acceptMeetingInvite(guest, inviteId),
                    () -> { meetingService.delete(owner, meetingId); return null; });

            Outcome<Object> accept = outcomes.get(0);
            Outcome<Object> delete = outcomes.get(1);
            assertTrue(delete.succeeded(), () -> "delete failed: " + delete.error());
            if (!accept.succeeded()) {
                // Only acceptable failure: the meeting was already gone when accept got the lock.
                assertInstanceOf(ResourceNotFoundException.class, accept.error(), () -> "accept: " + accept.error());
            }
            assertEquals(0, count("select count(*) from meetings where id = ?", meetingId));
            assertEquals(0, count("select count(*) from meeting_participants where meeting_id = ?", meetingId));
        }
    }
}
