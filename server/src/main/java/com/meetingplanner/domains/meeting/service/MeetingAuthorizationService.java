package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MeetingAuthorizationService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final GroupAuthorizationService groupAuthorizationService;

    public MeetingAuthorizationService(MeetingRepository meetingRepository,
                                       MeetingParticipantRepository meetingParticipantRepository,
                                       GroupAuthorizationService groupAuthorizationService) {
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.groupAuthorizationService = groupAuthorizationService;
    }

    public boolean canView(Long userId, Long meetingId) {
        Optional<MeetingEntity> meeting = meetingRepository.findById(meetingId);
        return meeting.map(m -> isParticipantOrCreator(userId, m)
                || (m.getGroup() != null && groupAuthorizationService.isMember(userId, m.getGroup().getId())))
                .orElse(false);
    }

    public boolean canManage(Long userId, Long meetingId) {
        Optional<MeetingEntity> meeting = meetingRepository.findById(meetingId);
        return meeting.map(m -> m.getCreator().getId().equals(userId)
                || (m.getGroup() != null && groupAuthorizationService.isAdminOrOwner(userId, m.getGroup().getId())))
                .orElse(false);
    }

    public boolean isParticipantOrCreator(Long userId, MeetingEntity meeting) {
        return meeting.getCreator().getId().equals(userId)
                || meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId);
    }
}