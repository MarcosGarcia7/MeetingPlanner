package com.meetingplanner.domains.group.service;

import com.meetingplanner.common.api.dto.GroupCreateRequest;
import com.meetingplanner.common.api.dto.GroupResponse;
import com.meetingplanner.common.api.dto.GroupUpdateRequest;
import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupAuthorizationService authorization;
    private final GroupMemberRepository groupMemberRepository;

    public GroupServiceImpl(GroupAuthorizationService authorization,
                            GroupMemberRepository groupMemberRepository) {
        this.authorization = authorization;
        this.groupMemberRepository = groupMemberRepository;
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
    public void setShareSchedule(long userId, long groupId, boolean shareSchedule) {
        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        member.setShareSchedule(shareSchedule);
        groupMemberRepository.save(member);
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