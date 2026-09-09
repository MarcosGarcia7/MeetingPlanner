package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.dto.MeetingCreateRequest;
import com.meetingplanner.common.api.dto.MeetingResponse;
import com.meetingplanner.common.api.dto.MeetingUpdateRequest;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.common.api.dto.ParticipantResponse;

import java.util.List;

public interface MeetingService {

    MeetingResponse create(long userId, MeetingCreateRequest request);

    MeetingResponse get(long userId, long meetingId);

    MeetingResponse update(long userId, long meetingId, MeetingUpdateRequest request);

    void delete(long userId, long meetingId);

    List<MeetingResponse> listForGroup(long userId, long groupId);

    List<MeetingResponse> listUpcoming(long userId);

    ParticipantResponse addParticipant(long actorUserId, long meetingId, MemberUsernameRequest request);

    void removeParticipant(long actorUserId, long meetingId, long targetUserId);

    InviteResponse invite(long actorUserId, long meetingId, MemberUsernameRequest request);
}