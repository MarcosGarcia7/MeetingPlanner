package com.meetingplanner.it;

import com.meetingplanner.common.api.dto.InviteBatchResponse;
import com.meetingplanner.common.api.dto.MeetingInviteBatchRequest;
import com.meetingplanner.common.api.dto.MeetingInviteRequest;
import com.meetingplanner.domains.meeting.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingInviteConcurrencyIT extends AbstractPostgresIT {

    private static final int ROUNDS = 10;

    @Autowired
    private MeetingService meetingService;

    @Test
    void concurrentInvitesToSameEmailOnNonGroupMeetingCreateOneInvite() throws Exception {
        long creator = insertUser("creator");
        MeetingInviteBatchRequest request = new MeetingInviteBatchRequest(
                List.of(new MeetingInviteRequest(null, "guest@example.com")));

        for (int round = 0; round < ROUNDS; round++) {
            long meetingId = insertMeeting(null, creator, OffsetDateTime.now().plusDays(3));

            List<Outcome<InviteBatchResponse>> outcomes = runConcurrently(
                    () -> meetingService.inviteBatch(creator, meetingId, request),
                    () -> meetingService.inviteBatch(creator, meetingId, request));

            long succeeded = outcomes.stream().filter(Outcome::succeeded).count();
            assertEquals(1, succeeded, () -> "outcomes: " + outcomes);
            for (Outcome<InviteBatchResponse> outcome : outcomes) {
                if (!outcome.succeeded()) {
                    // The loser sees the winner's invite and reports a 4xx, never a constraint 500.
                    ResponseStatusException e = assertInstanceOf(ResponseStatusException.class, outcome.error());
                    assertTrue(e.getStatusCode().is4xxClientError(), () -> "status: " + e.getStatusCode());
                }
            }
            assertEquals(1, count("select count(*) from meeting_invites where meeting_id = ?", meetingId));
        }
    }
}
