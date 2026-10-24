package com.meetingplanner.domains.group.service;

import com.meetingplanner.common.api.dto.*;

public interface GroupService {

    GroupResponse create(long userId, GroupCreateRequest request);

    GroupResponse get(long userId, long groupId);

    CursorPage<GroupSummaryResponse> listMine(long userId, String cursor, int size);

    CursorPage<GroupSummaryResponse> search(long viewerId, String query, String cursor, int size);

    GroupResponse update(long userId, long groupId, GroupUpdateRequest request);

    void delete(long userId, long groupId);

    void join(long userId, long groupId);

    InviteBatchResponse inviteBatch(long actorUserId, long groupId, GroupInviteBatchRequest request);

    CursorPage<InviteResponse> listInvites(long userId, long groupId, String cursor, int size);

    void removeMember(long actorUserId, long groupId, long targetUserId);

    void banMember(long actorUserId, long groupId, long targetUserId);

    void unbanMember(long actorUserId, long groupId, long targetUserId);

    CursorPage<GroupBanResponse> listBans(long actorUserId, long groupId, String cursor, int size);

    void promoteOrDemoteMember(long actorUserId, long groupId, long targetUserId);

    void transferOwnership(long actorUserId, long groupId, long targetUserId);

    void setShareSchedule(long userId, long groupId, boolean shareSchedule);

    void leave(long userId, long groupId);
}