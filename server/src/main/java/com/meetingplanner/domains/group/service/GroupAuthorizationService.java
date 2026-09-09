package com.meetingplanner.domains.group.service;

import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GroupAuthorizationService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupAuthorizationService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    public boolean isOwner(Long userId, Long groupId) {
        return groupRepository.findByOwnerIdAndId(userId, groupId).isPresent();
    }

    public boolean isAdmin(Long userId, Long groupId) {
        return member(groupId, userId).map(GroupMemberEntity::isAdmin).orElse(false);
    }

    public boolean isAdminOrOwner(Long userId, Long groupId) {
        return isOwner(userId, groupId) || isAdmin(userId, groupId);
    }

    public boolean isMember(Long userId, Long groupId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    private Optional<GroupMemberEntity> member(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
    }
}