package com.meetingplanner.domains.group.service;

import com.meetingplanner.common.api.dto.GroupCreateRequest;
import com.meetingplanner.common.api.dto.GroupResponse;
import com.meetingplanner.common.api.dto.GroupUpdateRequest;
import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupAuthorizationService authorization;

    public GroupServiceImpl(GroupAuthorizationService authorization) {
        this.authorization = authorization;
    }

    @Override
    @Transactional
    public GroupResponse create(long userId, GroupCreateRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse get(long userId, long groupId) {
        if (!authorization.isMember(userId, groupId) && !authorization.isOwner(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> listMine(long userId) {
        return List.of();
    }

    @Override
    @Transactional
    public GroupResponse update(long userId, long groupId, GroupUpdateRequest request) {
        if (!authorization.isAdminOrOwner(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void delete(long userId, long groupId) {
        if (!authorization.isOwner(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void invite(long userId, long groupId, MemberUsernameRequest request) {
        if (!authorization.isAdminOrOwner(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InviteResponse> listInvites(long userId, long groupId) {
        if (!authorization.isAdminOrOwner(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        return List.of();
    }

    @Override
    @Transactional
    public void removeMember(long actorUserId, long groupId, long targetUserId) {
        if (!authorization.isAdminOrOwner(actorUserId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void promoteMember(long actorUserId, long groupId, long targetUserId) {
        if (!authorization.isAdminOrOwner(actorUserId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void leave(long userId, long groupId) {
        if (!authorization.isMember(userId, groupId) && !authorization.isOwner(userId, groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }
}