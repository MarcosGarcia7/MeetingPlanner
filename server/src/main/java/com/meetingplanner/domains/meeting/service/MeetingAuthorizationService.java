package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import org.springframework.stereotype.Service;

@Service
public class MeetingAuthorizationService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final GroupAuthorizationService groupAuthorizationService;

    public MeetingAuthorizationService(MeetingParticipantRepository meetingParticipantRepository,
                                       GroupAuthorizationService groupAuthorizationService) {
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.groupAuthorizationService = groupAuthorizationService;
    }

    public boolean canView(Long userId, MeetingEntity m) {
        if (isParticipantOrCreator(userId, m)) {
            return true;
        }
        GroupEntity group = m.getGroup();
        if (group == null) {
            return false;
        }
        if (m.isPrivate()) {
            return groupAuthorizationService.isOwner(userId, group)
                    || groupAuthorizationService.isAdmin(userId, group);
        }
        if (group.isPrivate()) {
            return groupAuthorizationService.isOwner(userId, group)
                    || groupAuthorizationService.isMember(userId, group);
        }
        return true;
    }

    public boolean canManage(Long userId, MeetingEntity m) {
        GroupEntity group = m.getGroup();
        return m.getCreator().getId().equals(userId)
                || (group != null && groupAuthorizationService.isAdminOrOwner(userId, group));
    }

    public boolean isParticipantOrCreator(Long userId, MeetingEntity meeting) {
        return meeting.getCreator().getId().equals(userId)
                || meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId);
    }
}
