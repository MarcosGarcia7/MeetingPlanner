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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    void invalidatePendingBetweenUsesConditionalTransitions() {
        GroupInviteEntity groupInvite = new GroupInviteEntity();
        groupInvite.setId(3L);
        groupInvite.setStatus(InviteStatus.PENDING);
        MeetingInviteEntity meetingInvite = new MeetingInviteEntity();
        meetingInvite.setId(4L);
        meetingInvite.setStatus(InviteStatus.PENDING);
        MeetingJoinRequestEntity joinRequest = new MeetingJoinRequestEntity();
        joinRequest.setId(5L);
        joinRequest.setStatus(RequestStatus.PENDING);

        when(groupInviteRepository.findBetween(1L, 2L, InviteStatus.PENDING))
                .thenReturn(List.of(groupInvite));
        when(meetingInviteRepository.findBetween(1L, 2L, InviteStatus.PENDING))
                .thenReturn(List.of(meetingInvite));
        when(meetingJoinRequestRepository.findBetweenRequesterAndCreator(1L, 2L, RequestStatus.PENDING))
                .thenReturn(List.of(joinRequest));

        service.invalidatePendingBetween(1L, 2L);

        verify(groupInviteRepository).transitionStatus(eq(3L), eq(InviteStatus.PENDING),
                eq(InviteStatus.DECLINED), any(OffsetDateTime.class));
        verify(meetingInviteRepository).transitionStatus(eq(4L), eq(InviteStatus.PENDING),
                eq(InviteStatus.DECLINED), any(OffsetDateTime.class));
        verify(meetingJoinRequestRepository).transitionStatus(eq(5L), eq(RequestStatus.PENDING),
                eq(RequestStatus.CANCELLED), any(OffsetDateTime.class));
        // Managed entities are left untouched so a dirty-check flush can't overwrite a concurrent accept.
        assertEquals(InviteStatus.PENDING, groupInvite.getStatus());
        verify(groupInviteRepository, never()).saveAll(any());
        verify(meetingInviteRepository, never()).saveAll(any());
        verify(meetingJoinRequestRepository, never()).saveAll(any());
    }
}
