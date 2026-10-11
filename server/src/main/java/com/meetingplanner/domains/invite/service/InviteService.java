package com.meetingplanner.domains.invite.service;

import com.meetingplanner.common.api.dto.InviteResponse;

import java.util.List;

public interface InviteService {

    List<InviteResponse> listMine(long userId);

    InviteResponse acceptGroupInvite(long userId, long inviteId);

    InviteResponse declineGroupInvite(long userId, long inviteId);

    InviteResponse acceptMeetingInvite(long userId, long inviteId);

    InviteResponse declineMeetingInvite(long userId, long inviteId);

    InviteResponse viewGroupInvite(String token);

    InviteResponse viewMeetingInvite(String token);

    InviteResponse acceptGroupInviteByToken(String token);

    InviteResponse declineGroupInviteByToken(String token);

    InviteResponse acceptMeetingInviteByToken(String token);

    InviteResponse declineMeetingInviteByToken(String token);

    InviteResponse leaveMeetingInviteByToken(String token);
}