package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.InviteBatchResponse;
import com.meetingplanner.common.api.dto.MeetingInviteBatchRequest;
import com.meetingplanner.common.api.dto.MeetingSummaryResponse;
import com.meetingplanner.common.api.dto.MeetingInviteRequest;
import com.meetingplanner.common.api.dto.MeetingUpdateRequest;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingInviteEntity;
import com.meetingplanner.domains.meeting.repository.MeetingInviteRepository;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import com.meetingplanner.domains.user.service.UserProfileAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceImplTest {

    @Mock
    private MeetingAuthorizationService authorization;
    @Mock
    private GroupAuthorizationService groupAuthorization;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;
    @Mock
    private MeetingInviteRepository meetingInviteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileAuthorizationService userProfileAuthorization;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MeetingConflictService conflictService;
    @Mock
    private MeetingTimeChangeService timeChangeService;

    @InjectMocks
    private MeetingServiceImpl service;

    private static UserEntity user(long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static UserEntity verifiedUser(long id, String username) {
        UserEntity user = user(id, username);
        user.setEmail(username + "@example.com");
        user.setEmailVerifiedAt(OffsetDateTime.now());
        return user;
    }

    private static MeetingInviteBatchRequest meetingBatch(String... usernames) {
        return new MeetingInviteBatchRequest(
                Arrays.stream(usernames).map(name -> new MeetingInviteRequest(name, null)).toList());
    }

    private static MeetingEntity meeting(long id, UserEntity creator, UserEntity... participants) {
        MeetingEntity meeting = new MeetingEntity();
        meeting.setId(id);
        meeting.setCreator(creator);
        meeting.setTitle("Test meeting");
        meeting.setStartTime(OffsetDateTime.now().plusDays(7));
        for (UserEntity participant : participants) {
            meeting.addParticipant(participant);
        }
        return meeting;
    }

    @Test
    void searchUsesVisibleMatchesAndSummaryMapping() {
        UserEntity creator = user(1L, "alice");
        MeetingEntity meeting = meeting(10L, creator, creator);
        when(meetingRepository.searchVisible(1L, "%test%", null, null, PageRequest.of(0, 21)))
                .thenReturn(List.of(meeting));
        when(meetingParticipantRepository.countByMeetingIds(List.of(10L))).thenReturn(List.of());

        CursorPage<MeetingSummaryResponse> page = service.search(1L, " TEST ", null, 20);

        assertEquals(1, page.items().size());
        MeetingSummaryResponse result = page.items().getFirst();
        assertEquals(10L, result.id());
        assertEquals("Test meeting", result.title());
        assertEquals(1L, result.creatorId());
        assertEquals(0, result.participantCount());
        assertNull(page.nextCursor());
    }

    @Test
    void updateNotifiesEveryParticipantExceptActor() {
        UserEntity creator = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        UserEntity carol = user(3L, "carol");
        MeetingEntity meeting = meeting(10L, creator, creator, bob, carol);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(meeting)).thenReturn(meeting);

        service.update(1L, 10L, new MeetingUpdateRequest(null, null, "Room 2", null, null));

        verify(notificationService).push(eq(2L), eq("MEETING_UPDATED"), eq("meeting_updates"),
                eq("MEETING_UPDATED"), anyMap());
        verify(notificationService).push(eq(3L), eq("MEETING_UPDATED"), eq("meeting_updates"),
                eq("MEETING_UPDATED"), anyMap());
        verify(notificationService, never()).push(eq(1L), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void updateWithNoActualChangesDoesNotNotify() {
        UserEntity creator = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, creator, creator, bob);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(meeting)).thenReturn(meeting);

        service.update(1L, 10L, new MeetingUpdateRequest(null, null, null, null, null));

        verifyNoInteractions(notificationService);
    }

    @Test
    void updateWithTimeChangeAndOtherParticipantsCreatesProposal() {
        UserEntity creator = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, creator, creator, bob);
        OffsetDateTime originalStart = meeting.getStartTime();
        OffsetDateTime newStart = originalStart.plusDays(1);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(timeChangeService.hasPendingProposal(10L)).thenReturn(false);

        service.update(1L, 10L, new MeetingUpdateRequest(newStart, null, null, null, null));

        verify(timeChangeService).createProposal(eq(1L), eq(meeting), eq(newStart), isNull());
        assertEquals(originalStart, meeting.getStartTime());
    }

    @Test
    void updateWithTimeChangeAloneAppliesImmediately() {
        UserEntity creator = user(1L, "alice");
        MeetingEntity meeting = meeting(10L, creator, creator);
        OffsetDateTime newStart = meeting.getStartTime().plusDays(1);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(meeting)).thenReturn(meeting);

        service.update(1L, 10L, new MeetingUpdateRequest(newStart, null, null, null, null));

        assertEquals(newStart, meeting.getStartTime());
        verify(timeChangeService, never()).createProposal(anyLong(), any(), any(), any());
    }

    @Test
    void updateWithPendingProposalAndNewTimeChangeRejects() {
        UserEntity creator = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, creator, creator, bob);
        OffsetDateTime newStart = meeting.getStartTime().plusDays(1);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(timeChangeService.hasPendingProposal(10L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, 10L, new MeetingUpdateRequest(newStart, null, null, null, null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void deleteNotifiesParticipantsBeforeRemovingMeeting() {
        UserEntity creator = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, creator, creator, bob);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));

        service.delete(1L, 10L);

        InOrder order = inOrder(notificationService, meetingRepository);
        order.verify(notificationService).push(eq(2L), eq("MEETING_DELETED"), eq("meeting_updates"),
                eq("MEETING_DELETED"), anyMap());
        order.verify(meetingRepository).delete(meeting);
        verify(notificationService, never()).push(eq(1L), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void inviteBatchWithAllRecipientsFailingReturnsBadRequest() {
        UserEntity alice = user(1L, "alice");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.inviteBatch(1L, 10L, meetingBatch("ghost")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    void inviteBatchPropagatesPersistenceFailures() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(meetingInviteRepository.findByMeetingIdAndRecipientEmail(10L, "bob@example.com"))
                .thenReturn(Optional.empty());
        when(meetingInviteRepository.save(any(MeetingInviteEntity.class)))
                .thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class,
                () -> service.inviteBatch(1L, 10L, meetingBatch("bob")));
    }

    @Test
    void reinviteRefreshesTimeSent() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        MeetingInviteEntity existing = new MeetingInviteEntity();
        existing.setId(7L);
        existing.setStatus(InviteStatus.DECLINED);
        OffsetDateTime staleSentAt = OffsetDateTime.now().minusDays(2);
        existing.setTimeSent(staleSentAt);
        when(meetingInviteRepository.findByMeetingIdAndRecipientEmail(10L, "bob@example.com"))
                .thenReturn(Optional.of(existing));
        when(meetingInviteRepository.save(any(MeetingInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InviteBatchResponse response = service.inviteBatch(1L, 10L, meetingBatch("bob"));

        assertEquals(1, response.invited().size());
        assertEquals(InviteStatus.PENDING, existing.getStatus());
        assertTrue(existing.getTimeSent().isAfter(staleSentAt));
        verify(notificationService).queueEmailToForUser(eq(bob), eq("bob@example.com"), eq("MEETING_INVITE"), anyMap(), eq("meeting_invites"));
    }

    @Test
    void selfInviteIsRejectedBeforeEmailVerification() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(meetingInviteRepository.findByMeetingIdAndRecipientEmail(10L, "bob@example.com"))
                .thenReturn(Optional.empty());
        when(meetingInviteRepository.save(any(MeetingInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InviteBatchResponse response = service.inviteBatch(1L, 10L, meetingBatch("alice", "bob"));

        assertEquals(1, response.failed().size());
        assertEquals("You cannot invite yourself", response.failed().getFirst().reason());
    }
}
