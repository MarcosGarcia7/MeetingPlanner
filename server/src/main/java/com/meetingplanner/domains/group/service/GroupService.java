package com.meetingplanner.domains.group.service;

import com.meetingplanner.common.api.dto.GroupCreateRequest;
import com.meetingplanner.common.api.dto.GroupResponse;
import com.meetingplanner.common.api.dto.GroupUpdateRequest;
import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;

import java.util.List;

public interface GroupService {

    GroupResponse create(long userId, GroupCreateRequest request);

    GroupResponse get(long userId, long groupId);

    List<GroupResponse> listMine(long userId);

    GroupResponse update(long userId, long groupId, GroupUpdateRequest request);

    void delete(long userId, long groupId);

    void invite(long userId, long groupId, MemberUsernameRequest request);

    List<InviteResponse> listInvites(long userId, long groupId);

    void removeMember(long actorUserId, long groupId, long targetUserId);

    void promoteMember(long actorUserId, long groupId, long targetUserId);

    void leave(long userId, long groupId);
}