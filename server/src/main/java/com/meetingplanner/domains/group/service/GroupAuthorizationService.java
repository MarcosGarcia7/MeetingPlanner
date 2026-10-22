package com.meetingplanner.domains.group.service;

import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;

@Service
public class GroupAuthorizationService {

    private final GroupMemberRepository groupMemberRepository;

    public GroupAuthorizationService(GroupMemberRepository groupMemberRepository) {
        this.groupMemberRepository = groupMemberRepository;
    }

    public boolean isOwner(GroupMemberEntity member, GroupEntity group) {
        return group.getOwner().getId().equals(member.getUser().getId());
    }

    public boolean isAdminOrOwner(GroupMemberEntity member, GroupEntity group) {
        return isOwner(member, group) || member.isAdmin();
    }

    public boolean isOwner(Long userId, GroupEntity group) {
        return group.getOwner().getId().equals(userId);
    }

    public boolean isAdmin(Long userId, GroupEntity group) {
        return groupMemberRepository.findByGroupIdAndUserId(group.getId(), userId)
                .map(GroupMemberEntity::isAdmin)
                .orElse(false);
    }

    public boolean isAdminOrOwner(Long userId, GroupEntity group) {
        return isOwner(userId, group) || isAdmin(userId, group);
    }

    public boolean isMember(Long userId, GroupEntity group) {
        return groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId);
    }

}
