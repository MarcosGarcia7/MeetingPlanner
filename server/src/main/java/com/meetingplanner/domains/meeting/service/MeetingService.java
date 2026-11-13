package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.InviteBatchResponse;
import com.meetingplanner.common.api.dto.InviteCheckResponse;
import com.meetingplanner.common.api.dto.MeetingCreateRequest;
import com.meetingplanner.common.api.dto.MeetingDraftCheckRequest;
import com.meetingplanner.common.api.dto.MeetingDraftCheckResponse;
import com.meetingplanner.common.api.dto.MeetingInviteBatchRequest;
import com.meetingplanner.common.api.dto.MeetingResponse;
import com.meetingplanner.common.api.dto.MeetingSummaryResponse;
import com.meetingplanner.common.api.dto.MeetingUpdateRequest;
import com.meetingplanner.common.api.dto.TimeChangeCheckRequest;
import com.meetingplanner.common.api.model.MeetingTimeframe;

import java.util.List;

public interface MeetingService {

    MeetingResponse create(long userId, MeetingCreateRequest request);

    MeetingResponse createInGroup(long userId, long groupId, MeetingCreateRequest request);

    MeetingDraftCheckResponse checkDraft(long userId, MeetingDraftCheckRequest request);

    MeetingDraftCheckResponse checkTimeChange(long userId, long meetingId, TimeChangeCheckRequest request);

    MeetingResponse get(long userId, long meetingId);

    MeetingResponse update(long userId, long meetingId, MeetingUpdateRequest request);

    void delete(long userId, long meetingId);

    CursorPage<MeetingSummaryResponse> listMine(long userId, Long groupId, MeetingTimeframe when, String cursor, int size);

    CursorPage<MeetingSummaryResponse> search(long viewerId, String query, String cursor, int size);

    CursorPage<MeetingSummaryResponse> listCreated(long userId, String cursor, int size);

    CursorPage<MeetingSummaryResponse> listForGroup(long userId, long groupId, String cursor, int size);

    CursorPage<MeetingSummaryResponse> listSchedule(long viewerId, long targetUserId, String cursor, int size);

    List<MeetingSummaryResponse> listUpcoming(long userId);

    void removeParticipant(long actorUserId, long meetingId, long targetUserId);

    void leaveMeeting(long userId, long meetingId);

    InviteBatchResponse inviteBatch(long actorUserId, long meetingId, MeetingInviteBatchRequest request);

    InviteCheckResponse checkInvites(long actorUserId, long meetingId, MeetingInviteBatchRequest request);

    /**
     * Sends a regular meeting invite email to an address entered on a share link's guest form, as if the
     * link's creator had invited it. A pending invite for the address is re-sent with a fresh token. Domain
     * failures (already a participant, blocked, banned) are swallowed so the caller can answer the same way
     * whatever the outcome; returns whether an invite was sent.
     */
    boolean inviteFromShareLink(long senderUserId, long meetingId, String email);
}
