package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.dto.MeetingCreateRequest;
import com.meetingplanner.common.api.dto.MeetingResponse;
import com.meetingplanner.common.api.dto.MeetingUpdateRequest;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.common.api.dto.ParticipantResponse;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MeetingServiceImpl implements MeetingService {

    private final MeetingAuthorizationService authorization;

    public MeetingServiceImpl(MeetingAuthorizationService authorization) {
        this.authorization = authorization;
    }

    @Override
    @Transactional
    public MeetingResponse create(long userId, MeetingCreateRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingResponse get(long userId, long meetingId) {
        if (!authorization.canView(userId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public MeetingResponse update(long userId, long meetingId, MeetingUpdateRequest request) {
        if (!authorization.canManage(userId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void delete(long userId, long meetingId) {
        if (!authorization.canManage(userId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> listForGroup(long userId, long groupId) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> listUpcoming(long userId) {
        return List.of();
    }

    @Override
    @Transactional
    public ParticipantResponse addParticipant(long actorUserId, long meetingId, MemberUsernameRequest request) {
        if (!authorization.canManage(actorUserId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void removeParticipant(long actorUserId, long meetingId, long targetUserId) {
        if (!authorization.canManage(actorUserId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public InviteResponse invite(long actorUserId, long meetingId, MemberUsernameRequest request) {
        if (!authorization.canManage(actorUserId, meetingId)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }
}