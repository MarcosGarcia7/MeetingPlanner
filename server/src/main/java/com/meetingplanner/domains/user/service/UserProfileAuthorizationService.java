package com.meetingplanner.domains.user.service;

import com.meetingplanner.domains.friend.repository.FriendshipRepository;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserProfileAuthorizationService {

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final FriendshipRepository friendshipRepository;

    public UserProfileAuthorizationService(UserRepository userRepository,
                                           GroupMemberRepository groupMemberRepository,
                                           FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.friendshipRepository = friendshipRepository;
    }

    public boolean canViewProfile(Long viewerId, Long targetUserId) {
        UserEntity target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) {
            return false;
        }
        if (target.getId().equals(viewerId)) {
            return true;
        }
        if (!target.isPrivate()) {
            return true;
        }
        return sharesContext(viewerId, targetUserId, target);
    }

    private boolean sharesContext(Long viewerId, Long targetUserId, UserEntity target) {
        if (target.isFriendsCanViewPrivate()
                && friendshipRepository.areFriends(viewerId, targetUserId)) {
            return true;
        }

        Set<Long> viewerGroupIds = groupMemberRepository.findByUserId(viewerId).stream()
                .map(member -> member.getGroup().getId())
                .collect(Collectors.toSet());

        return groupMemberRepository.findByUserId(targetUserId).stream()
                .filter(GroupMemberEntity::isShareSchedule)
                .map(member -> member.getGroup().getId())
                .anyMatch(viewerGroupIds::contains);
    }
}