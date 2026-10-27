package com.meetingplanner.domains.request.service;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.JoinRequestResponse;
import com.meetingplanner.common.api.model.RequestStatus;
import com.meetingplanner.common.api.model.TargetKind;

public interface JoinRequestService {

    JoinRequestResponse requestGroupJoin(long userId, long groupId);

    JoinRequestResponse requestMeetingJoin(long userId, long meetingId);

    CursorPage<JoinRequestResponse> listMine(long userId, RequestStatus status, TargetKind kind, String cursor, int size);

    CursorPage<JoinRequestResponse> listForGroup(long actorId, long groupId, RequestStatus status, String cursor, int size);

    CursorPage<JoinRequestResponse> listForMeeting(long actorId, long meetingId, String cursor, int size);

    JoinRequestResponse approveGroup(long actorId, long requestId);

    JoinRequestResponse declineGroup(long actorId, long requestId);

    JoinRequestResponse approveMeeting(long actorId, long requestId);

    JoinRequestResponse declineMeeting(long actorId, long requestId);

    JoinRequestResponse cancelGroup(long userId, long requestId);

    JoinRequestResponse cancelMeeting(long userId, long requestId);
}
