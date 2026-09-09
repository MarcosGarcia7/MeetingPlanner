package com.meetingplanner.domains.user.service;

import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.meeting.model.MeetingParticipantEntity;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserProfileAuthorizationService {

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;

    public UserProfileAuthorizationService(UserRepository userRepository, GroupMemberRepository groupMemberRepository,
                                           MeetingParticipantRepository meetingParticipantRepository) {
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
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
        return sharesContext(viewerId, targetUserId);
    }

    private boolean sharesContext(Long viewerId, Long targetUserId) {
        Set<Long> viewerGroups = groupMemberRepository.findByUserId(viewerId).stream()
                .map(member -> member.getGroup().getId())
                .collect(Collectors.toSet());
        boolean sharedGroup = groupMemberRepository.findByUserId(targetUserId).stream()
                .map(GroupMemberEntity::getGroup)
                .anyMatch(group -> viewerGroups.contains(group.getId()));
        if (sharedGroup) {
            return true;
        }

        Set<Long> viewerMeetings = meetingParticipantRepository.findByUserId(viewerId).stream()
                .map(participant -> participant.getMeeting().getId())
                .collect(Collectors.toSet());
        return meetingParticipantRepository.findByUserId(targetUserId).stream()
                .map(MeetingParticipantEntity::getMeeting)
                .anyMatch(meeting -> viewerMeetings.contains(meeting.getId()));
    }
}