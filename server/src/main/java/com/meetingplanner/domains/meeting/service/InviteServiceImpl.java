package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.dto.InviteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InviteServiceImpl implements InviteService {

    @Override
    @Transactional(readOnly = true)
    public List<InviteResponse> listMine(long userId) {
        return List.of();
    }

    @Override
    @Transactional
    public InviteResponse acceptGroupInvite(long userId, long inviteId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public InviteResponse declineGroupInvite(long userId, long inviteId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public InviteResponse acceptMeetingInvite(long userId, long inviteId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public InviteResponse declineMeetingInvite(long userId, long inviteId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}