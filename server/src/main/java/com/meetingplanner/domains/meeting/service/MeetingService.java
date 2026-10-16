package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.InviteBatchResponse;
import com.meetingplanner.common.api.dto.InviteCheckResponse;
import com.meetingplanner.common.api.dto.MeetingCreateRequest;
import com.meetingplanner.common.api.dto.MeetingInviteBatchRequest;
import com.meetingplanner.common.api.dto.MeetingResponse;
import com.meetingplanner.common.api.dto.MeetingSummaryResponse;
import com.meetingplanner.common.api.dto.MeetingUpdateRequest;

import java.util.List;

public interface MeetingService {

    MeetingResponse create(long userId, MeetingCreateRequest request);

    MeetingResponse createInGroup(long userId, long groupId, MeetingCreateRequest request);

    MeetingResponse get(long userId, long meetingId);

    MeetingResponse update(long userId, long meetingId, MeetingUpdateRequest request);

    void delete(long userId, long meetingId);

    CursorPage<MeetingSummaryResponse> listMine(long userId, Long groupId, String cursor, int size);

    CursorPage<MeetingSummaryResponse> listCreated(long userId, String cursor, int size);

    CursorPage<MeetingSummaryResponse> listForGroup(long userId, long groupId, String cursor, int size);

    CursorPage<MeetingSummaryResponse> listSchedule(long viewerId, long targetUserId, String cursor, int size);

    List<MeetingSummaryResponse> listUpcoming(long userId);

    void removeParticipant(long actorUserId, long meetingId, long targetUserId);

    void leaveMeeting(long userId, long meetingId);

    InviteBatchResponse inviteBatch(long actorUserId, long meetingId, MeetingInviteBatchRequest request);

    InviteCheckResponse checkInvites(long actorUserId, long meetingId, MeetingInviteBatchRequest request);
}
