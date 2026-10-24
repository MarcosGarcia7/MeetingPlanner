package com.meetingplanner.domains.friend.service;

import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.api.model.RequestStatus;
import com.meetingplanner.domains.friend.model.FriendshipStatus;
import com.meetingplanner.domains.friend.repository.FriendshipRepository;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import com.meetingplanner.domains.group.repository.GroupInviteRepository;
import com.meetingplanner.domains.meeting.model.MeetingInviteEntity;
import com.meetingplanner.domains.meeting.model.MeetingJoinRequestEntity;
import com.meetingplanner.domains.meeting.repository.MeetingInviteRepository;
import com.meetingplanner.domains.meeting.repository.MeetingJoinRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
public class BlockService {

    private final FriendshipRepository friendshipRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final MeetingInviteRepository meetingInviteRepository;
    private final MeetingJoinRequestRepository meetingJoinRequestRepository;

    public BlockService(FriendshipRepository friendshipRepository,
                        GroupInviteRepository groupInviteRepository,
                        MeetingInviteRepository meetingInviteRepository,
                        MeetingJoinRequestRepository meetingJoinRequestRepository) {
        this.friendshipRepository = friendshipRepository;
        this.groupInviteRepository = groupInviteRepository;
        this.meetingInviteRepository = meetingInviteRepository;
        this.meetingJoinRequestRepository = meetingJoinRequestRepository;
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(long a, long b) {
        if (a == b) {
            return false;
        }
        long low = Math.min(a, b);
        long high = Math.max(a, b);
        return friendshipRepository.findByUserIdAndFriendId(low, high)
                .map(friendship -> friendship.getStatus() == FriendshipStatus.BLOCKED)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public void assertNotBlocked(long a, long b, String message) {
        if (isBlocked(a, b)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    @Transactional
    public void invalidatePendingBetween(long a, long b) {
        if (a == b) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        // Conditional per-row transitions instead of entity saves, so an invite or request accepted
        // concurrently is left alone rather than overwritten back to DECLINED/CANCELLED.
        for (GroupInviteEntity invite : groupInviteRepository.findBetween(a, b, InviteStatus.PENDING)) {
            groupInviteRepository.transitionStatus(invite.getId(), InviteStatus.PENDING, InviteStatus.DECLINED, now);
        }
        for (MeetingInviteEntity invite : meetingInviteRepository.findBetween(a, b, InviteStatus.PENDING)) {
            meetingInviteRepository.transitionStatus(invite.getId(), InviteStatus.PENDING, InviteStatus.DECLINED, now);
        }
        for (MeetingJoinRequestEntity request :
                meetingJoinRequestRepository.findBetweenRequesterAndCreator(a, b, RequestStatus.PENDING)) {
            meetingJoinRequestRepository.transitionStatus(
                    request.getId(), RequestStatus.PENDING, RequestStatus.CANCELLED, now);
        }
    }
}
