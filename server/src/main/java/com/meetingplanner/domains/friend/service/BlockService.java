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
import java.util.List;

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

        List<GroupInviteEntity> groupInvites = groupInviteRepository.findBetween(a, b, InviteStatus.PENDING);
        for (GroupInviteEntity invite : groupInvites) {
            invite.setStatus(InviteStatus.DECLINED);
            invite.setRespondedAt(now);
        }
        groupInviteRepository.saveAll(groupInvites);

        List<MeetingInviteEntity> meetingInvites = meetingInviteRepository.findBetween(a, b, InviteStatus.PENDING);
        for (MeetingInviteEntity invite : meetingInvites) {
            invite.setStatus(InviteStatus.DECLINED);
            invite.setRespondedAt(now);
        }
        meetingInviteRepository.saveAll(meetingInvites);

        List<MeetingJoinRequestEntity> joinRequests =
                meetingJoinRequestRepository.findBetweenRequesterAndCreator(a, b, RequestStatus.PENDING);
        for (MeetingJoinRequestEntity request : joinRequests) {
            request.setStatus(RequestStatus.CANCELLED);
            request.setRespondedAt(now);
        }
        meetingJoinRequestRepository.saveAll(joinRequests);
    }
}
