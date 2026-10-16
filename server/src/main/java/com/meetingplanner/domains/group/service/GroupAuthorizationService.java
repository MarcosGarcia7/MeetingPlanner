package com.meetingplanner.domains.group.service;

import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GroupAuthorizationService {

    private final GroupMemberRepository groupMemberRepository;

    public GroupAuthorizationService(GroupMemberRepository groupMemberRepository) {
        this.groupMemberRepository = groupMemberRepository;
    }

    public boolean isOwner(Long userId, GroupEntity group) {
        return group.getOwner().getId().equals(userId);
    }

    public boolean isAdmin(Long userId, GroupEntity group) {
        return member(group.getId(), userId).map(GroupMemberEntity::isAdmin).orElse(false);
    }

    public boolean isAdminOrOwner(Long userId, GroupEntity group) {
        return isOwner(userId, group) || isAdmin(userId, group);
    }

    public boolean isMember(Long userId, GroupEntity group) {
        return groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId);
    }

    private Optional<GroupMemberEntity> member(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
    }
}
