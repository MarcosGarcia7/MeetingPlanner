package com.meetingplanner.domains.friend.service;

import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.api.model.RequestStatus;
import com.meetingplanner.domains.friend.model.FriendshipEntity;
import com.meetingplanner.domains.friend.model.FriendshipStatus;
import com.meetingplanner.domains.friend.repository.FriendshipRepository;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import com.meetingplanner.domains.group.repository.GroupInviteRepository;
import com.meetingplanner.domains.meeting.model.MeetingInviteEntity;
import com.meetingplanner.domains.meeting.model.MeetingJoinRequestEntity;
import com.meetingplanner.domains.meeting.repository.MeetingInviteRepository;
import com.meetingplanner.domains.meeting.repository.MeetingJoinRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private GroupInviteRepository groupInviteRepository;
    @Mock
    private MeetingInviteRepository meetingInviteRepository;
    @Mock
    private MeetingJoinRequestRepository meetingJoinRequestRepository;

    @InjectMocks
    private BlockService service;

    @Test
    void isBlockedLooksUpSortedPairAndIsBidirectional() {
        FriendshipEntity block = new FriendshipEntity();
        block.setStatus(FriendshipStatus.BLOCKED);
        when(friendshipRepository.findByUserIdAndFriendId(1L, 2L)).thenReturn(Optional.of(block));

        assertTrue(service.isBlocked(2L, 1L));
        assertTrue(service.isBlocked(1L, 2L));
    }

    @Test
    void nonBlockedStatusIsNotBlocked() {
        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        when(friendshipRepository.findByUserIdAndFriendId(1L, 2L)).thenReturn(Optional.of(friendship));

        assertFalse(service.isBlocked(1L, 2L));
    }

    @Test
    void invalidatePendingBetweenCancelsInvitesAndJoinRequests() {
        GroupInviteEntity groupInvite = new GroupInviteEntity();
        groupInvite.setStatus(InviteStatus.PENDING);
        MeetingInviteEntity meetingInvite = new MeetingInviteEntity();
        meetingInvite.setStatus(InviteStatus.PENDING);
        MeetingJoinRequestEntity joinRequest = new MeetingJoinRequestEntity();
        joinRequest.setStatus(RequestStatus.PENDING);

        when(groupInviteRepository.findBetween(1L, 2L, InviteStatus.PENDING))
                .thenReturn(List.of(groupInvite));
        when(meetingInviteRepository.findBetween(1L, 2L, InviteStatus.PENDING))
                .thenReturn(List.of(meetingInvite));
        when(meetingJoinRequestRepository.findBetweenRequesterAndCreator(1L, 2L, RequestStatus.PENDING))
                .thenReturn(List.of(joinRequest));

        service.invalidatePendingBetween(1L, 2L);

        assertEquals(InviteStatus.DECLINED, groupInvite.getStatus());
        assertEquals(InviteStatus.DECLINED, meetingInvite.getStatus());
        assertEquals(RequestStatus.CANCELLED, joinRequest.getStatus());
        assertNotNull(groupInvite.getRespondedAt());
        assertNotNull(meetingInvite.getRespondedAt());
        assertNotNull(joinRequest.getRespondedAt());
        verify(groupInviteRepository).saveAll(List.of(groupInvite));
        verify(meetingInviteRepository).saveAll(List.of(meetingInvite));
        verify(meetingJoinRequestRepository).saveAll(List.of(joinRequest));
    }
}
