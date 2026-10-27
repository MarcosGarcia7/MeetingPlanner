package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.InviteBatchResponse;
import com.meetingplanner.common.api.dto.MeetingInviteBatchRequest;
import com.meetingplanner.common.api.dto.MeetingSummaryResponse;
import com.meetingplanner.common.api.dto.MeetingInviteRequest;
import com.meetingplanner.common.api.dto.MeetingResponse;
import com.meetingplanner.common.api.dto.MeetingUpdateRequest;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.api.model.MeetingTimeframe;
import com.meetingplanner.common.api.model.MeetingViewerStatus;
import com.meetingplanner.common.api.model.RequestStatus;
import com.meetingplanner.domains.friend.service.BlockService;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingInviteEntity;
import com.meetingplanner.domains.meeting.model.MeetingJoinRequestEntity;
import com.meetingplanner.domains.meeting.repository.MeetingInviteRepository;
import com.meetingplanner.domains.meeting.repository.MeetingJoinRequestRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

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
    private MeetingJoinRequestRepository meetingJoinRequestRepository;
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
    @Mock
    private BlockService blockService;

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
    void listMineUpcomingUsesAscendingQueryFromNow() {
        OffsetDateTime before = OffsetDateTime.now();
        when(meetingRepository.listMineUpcoming(eq(1L), isNull(), any(OffsetDateTime.class), isNull(), isNull(),
                eq(PageRequest.of(0, 21)))).thenReturn(List.of());

        service.listMine(1L, null, MeetingTimeframe.UPCOMING, null, 20);

        verify(meetingRepository).listMineUpcoming(eq(1L), isNull(),
                argThat(now -> !now.isBefore(before)), isNull(), isNull(), eq(PageRequest.of(0, 21)));
        verify(meetingRepository, never()).listMine(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void listMinePastBoundsByNowAndDefaultListsEverything() {
        when(meetingRepository.listMine(eq(1L), eq(5L), any(), isNull(), isNull(), eq(PageRequest.of(0, 21))))
                .thenReturn(List.of());

        service.listMine(1L, 5L, MeetingTimeframe.PAST, null, 20);
        service.listMine(1L, 5L, null, null, 20);

        verify(meetingRepository).listMine(eq(1L), eq(5L), any(OffsetDateTime.class), isNull(), isNull(),
                eq(PageRequest.of(0, 21)));
        verify(meetingRepository).listMine(eq(1L), eq(5L), isNull(), isNull(), isNull(), eq(PageRequest.of(0, 21)));
        verify(meetingRepository, never()).listMineUpcoming(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void getReportsGroupNameAndWhetherViewerCanManage() {
        UserEntity creator = user(1L, "alice");
        UserEntity viewer = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, creator, creator, viewer);
        GroupEntity group = new GroupEntity();
        group.setId(5L);
        group.setGroupName("Book club");
        meeting.setGroup(group);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(authorization.canView(2L, meeting)).thenReturn(true);
        when(authorization.canManage(2L, meeting)).thenReturn(false);
        when(authorization.canView(1L, meeting)).thenReturn(true);
        when(authorization.canManage(1L, meeting)).thenReturn(true);

        MeetingResponse asParticipant = service.get(2L, 10L);
        MeetingResponse asCreator = service.get(1L, 10L);

        assertEquals("Book club", asParticipant.groupName());
        assertFalse(asParticipant.canManage());
        assertTrue(asCreator.canManage());
    }

    // A public meeting in a public group that bob (2) can view but isn't in.
    private MeetingEntity publicGroupMeeting(GroupEntity group) {
        MeetingEntity meeting = meeting(10L, user(1L, "alice"), user(1L, "alice"));
        meeting.setGroup(group);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(authorization.canView(2L, meeting)).thenReturn(true);
        return meeting;
    }

    private static GroupEntity group(long id) {
        GroupEntity group = new GroupEntity();
        group.setId(id);
        group.setGroupName("Book club");
        group.setPrivate(false);
        return group;
    }

    @Test
    void getReportsParticipantWithoutLookingUpInvitesOrRequests() {
        UserEntity creator = user(1L, "alice");
        UserEntity viewer = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, creator, creator, viewer);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(authorization.canView(2L, meeting)).thenReturn(true);

        MeetingResponse response = service.get(2L, 10L);

        assertEquals(MeetingViewerStatus.PARTICIPANT, response.viewerStatus());
        assertNull(response.pendingRequestId());
        assertNull(response.pendingInviteId());
        verifyNoInteractions(meetingInviteRepository, meetingJoinRequestRepository, blockService);
    }

    @Test
    void getReportsPendingInvite() {
        publicGroupMeeting(group(5L));
        MeetingInviteEntity invite = new MeetingInviteEntity();
        invite.setId(30L);
        when(meetingInviteRepository.findFirstByMeetingIdAndRecipientIdAndStatus(10L, 2L, InviteStatus.PENDING))
                .thenReturn(Optional.of(invite));

        MeetingResponse response = service.get(2L, 10L);

        assertEquals(MeetingViewerStatus.INVITED, response.viewerStatus());
        assertEquals(30L, response.pendingInviteId());
        assertNull(response.pendingRequestId());
        verifyNoInteractions(meetingJoinRequestRepository);
    }

    @Test
    void getReportsPendingRequest() {
        publicGroupMeeting(group(5L));
        MeetingJoinRequestEntity request = new MeetingJoinRequestEntity();
        request.setId(40L);
        when(meetingJoinRequestRepository.findFirstByMeetingIdAndRequesterIdAndStatus(10L, 2L, RequestStatus.PENDING))
                .thenReturn(Optional.of(request));

        MeetingResponse response = service.get(2L, 10L);

        assertEquals(MeetingViewerStatus.REQUESTED, response.viewerStatus());
        assertEquals(40L, response.pendingRequestId());
        assertNull(response.pendingInviteId());
    }

    @Test
    void getReportsCanRequestForPublicMeetingInPublicGroup() {
        publicGroupMeeting(group(5L));

        assertEquals(MeetingViewerStatus.CAN_REQUEST, service.get(2L, 10L).viewerStatus());
    }

    @Test
    void getReportsCanRequestForVisiblePrivateMeeting() {
        MeetingEntity meeting = publicGroupMeeting(group(5L));
        meeting.setPrivate(true);

        assertEquals(MeetingViewerStatus.CAN_REQUEST, service.get(2L, 10L).viewerStatus());
    }

    @Test
    void getReportsCanRequestForMeetingInPrivateGroup() {
        GroupEntity group = group(5L);
        group.setPrivate(true);
        publicGroupMeeting(group);

        assertEquals(MeetingViewerStatus.CAN_REQUEST, service.get(2L, 10L).viewerStatus());
    }

    @Test
    void getReportsUnavailableForEndedMeeting() {
        MeetingEntity meeting = publicGroupMeeting(group(5L));
        meeting.setStartTime(OffsetDateTime.now().minusHours(2));
        meeting.setEndTime(OffsetDateTime.now().minusHours(1));

        assertEquals(MeetingViewerStatus.UNAVAILABLE, service.get(2L, 10L).viewerStatus());
    }

    @Test
    void getReportsUnavailableWhenBannedFromGroup() {
        GroupEntity group = group(5L);
        publicGroupMeeting(group);
        when(groupAuthorization.isBanned(2L, group)).thenReturn(true);

        assertEquals(MeetingViewerStatus.UNAVAILABLE, service.get(2L, 10L).viewerStatus());
    }

    @Test
    void getReportsUnavailableWhenBlockedWithCreator() {
        publicGroupMeeting(group(5L));
        when(blockService.isBlocked(2L, 1L)).thenReturn(true);

        assertEquals(MeetingViewerStatus.UNAVAILABLE, service.get(2L, 10L).viewerStatus());
    }

    @Test
    void getHidesMeetingViewerCannotSee() {
        MeetingEntity meeting = meeting(10L, user(1L, "alice"));
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));

        assertThrows(ResourceNotFoundException.class, () -> service.get(3L, 10L));
    }

    @Test
    void updateNotifiesEveryParticipantExceptActor() {
        UserEntity creator = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        UserEntity carol = user(3L, "carol");
        MeetingEntity meeting = meeting(10L, creator, creator, bob, carol);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
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
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
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
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
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
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
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
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
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
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));

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
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.inviteBatch(1L, 10L, meetingBatch("ghost")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Could not invite @ghost: User not found", exception.getReason());
        verifyNoInteractions(notificationService);
    }

    @Test
    void inviteBatchWithAllRecipientsFailingListsEveryReason() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(authorization.isParticipantOrCreator(2L, meeting)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.inviteBatch(1L, 10L, meetingBatch("ghost", "bob")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("No invites could be sent. @ghost: User not found; "
                + "@bob: User is already a participant of this meeting", exception.getReason());
    }

    @Test
    void existingParticipantWithoutInviteIsRejected() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(authorization.isParticipantOrCreator(2L, meeting)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.inviteBatch(1L, 10L, meetingBatch("bob")));

        assertEquals("Could not invite @bob: User is already a participant of this meeting", exception.getReason());
        verify(meetingInviteRepository, never()).findByMeetingIdAndRecipientEmail(10L, "bob@example.com");
        verifyNoInteractions(notificationService);
    }

    @Test
    void reinviteAfterRemovalReusesAcceptedInvite() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        MeetingInviteEntity existing = new MeetingInviteEntity();
        existing.setId(7L);
        existing.setStatus(InviteStatus.ACCEPTED);
        existing.setRecipient(bob);
        existing.setRespondedAt(OffsetDateTime.now().minusDays(1));
        when(meetingInviteRepository.findByMeetingIdAndRecipientEmail(10L, "bob@example.com"))
                .thenReturn(Optional.of(existing));
        when(meetingInviteRepository.saveAndFlush(any(MeetingInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InviteBatchResponse response = service.inviteBatch(1L, 10L, meetingBatch("bob"));

        assertEquals(1, response.invited().size());
        assertEquals(7L, response.invited().getFirst().id());
        assertEquals(InviteStatus.PENDING, existing.getStatus());
        assertNull(existing.getRespondedAt());
        verify(notificationService).queueEmailToForUser(eq(bob), eq("bob@example.com"), eq("MEETING_INVITE"), anyMap(), eq("meeting_invites"));
        // The in-app copy never carries the tokenized invite link.
        verify(notificationService).pushInApp(eq(2L), eq("MEETING_INVITE"), eq("meeting_invites"),
                argThat(vars -> "alice".equals(vars.get("sender")) && !vars.containsKey("link")));
    }

    @Test
    void emailInviteToUnregisteredAddressSendsNoInAppNotification() {
        UserEntity alice = user(1L, "alice");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByEmail("dave@example.com")).thenReturn(Optional.empty());
        when(meetingInviteRepository.findByMeetingIdAndRecipientEmail(10L, "dave@example.com"))
                .thenReturn(Optional.empty());
        when(meetingInviteRepository.saveAndFlush(any(MeetingInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.inviteBatch(1L, 10L, new MeetingInviteBatchRequest(
                List.of(new MeetingInviteRequest(null, "dave@example.com"))));

        verify(notificationService).queueEmailToForUser(isNull(), eq("dave@example.com"), eq("MEETING_INVITE"),
                anyMap(), eq("meeting_invites"));
        verify(notificationService, never()).pushInApp(anyLong(), anyString(), anyString(), anyMap());
    }

    @Test
    void emailReinviteWithAcceptedInviteOfCurrentParticipantIsRejected() {
        UserEntity alice = user(1L, "alice");
        UserEntity carol = user(3L, "carol");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByEmail("carol@example.com")).thenReturn(Optional.empty());

        MeetingInviteEntity existing = new MeetingInviteEntity();
        existing.setId(8L);
        existing.setStatus(InviteStatus.ACCEPTED);
        existing.setRecipient(carol);
        when(meetingInviteRepository.findByMeetingIdAndRecipientEmail(10L, "carol@example.com"))
                .thenReturn(Optional.of(existing));
        when(authorization.isParticipantOrCreator(3L, meeting)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.inviteBatch(1L, 10L, new MeetingInviteBatchRequest(
                        List.of(new MeetingInviteRequest(null, "carol@example.com")))));

        assertEquals("Could not invite carol@example.com: User is already a participant of this meeting",
                exception.getReason());
        verify(meetingInviteRepository, never()).saveAndFlush(any(MeetingInviteEntity.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void inviteBatchPropagatesPersistenceFailures() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(meetingInviteRepository.findByMeetingIdAndRecipientEmail(10L, "bob@example.com"))
                .thenReturn(Optional.empty());
        when(meetingInviteRepository.saveAndFlush(any(MeetingInviteEntity.class)))
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
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        MeetingInviteEntity existing = new MeetingInviteEntity();
        existing.setId(7L);
        existing.setStatus(InviteStatus.DECLINED);
        OffsetDateTime staleSentAt = OffsetDateTime.now().minusDays(2);
        existing.setTimeSent(staleSentAt);
        when(meetingInviteRepository.findByMeetingIdAndRecipientEmail(10L, "bob@example.com"))
                .thenReturn(Optional.of(existing));
        when(meetingInviteRepository.saveAndFlush(any(MeetingInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InviteBatchResponse response = service.inviteBatch(1L, 10L, meetingBatch("bob"));

        assertEquals(1, response.invited().size());
        assertEquals(InviteStatus.PENDING, existing.getStatus());
        assertTrue(existing.getTimeSent().isAfter(staleSentAt));
        verify(notificationService).queueEmailToForUser(eq(bob), eq("bob@example.com"), eq("MEETING_INVITE"), anyMap(), eq("meeting_invites"));
    }

    @Test
    void bannedRecipientOfGroupMeetingIsRejected() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        GroupEntity group = new GroupEntity();
        group.setId(4L);
        MeetingEntity meeting = meeting(10L, alice);
        meeting.setGroup(group);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(groupRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(groupAuthorization.isBanned(2L, group)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.inviteBatch(1L, 10L, meetingBatch("bob")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(meetingInviteRepository, never()).save(any(MeetingInviteEntity.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void blockedRecipientIsRejected() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot invite this user"))
                .when(blockService).assertNotBlocked(1L, 2L, "You cannot invite this user");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.inviteBatch(1L, 10L, meetingBatch("bob")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(meetingInviteRepository, never()).save(any(MeetingInviteEntity.class));
    }

    @Test
    void selfInviteIsRejectedBeforeEmailVerification() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(meetingInviteRepository.findByMeetingIdAndRecipientEmail(10L, "bob@example.com"))
                .thenReturn(Optional.empty());
        when(meetingInviteRepository.saveAndFlush(any(MeetingInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InviteBatchResponse response = service.inviteBatch(1L, 10L, meetingBatch("alice", "bob"));

        assertEquals(1, response.failed().size());
        assertEquals("You cannot invite yourself", response.failed().getFirst().reason());
    }

    @Test
    void inviteBatchLocksGroupBeforeMeeting() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        GroupEntity group = new GroupEntity();
        group.setId(4L);
        MeetingEntity meeting = meeting(10L, alice);
        meeting.setGroup(group);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(groupRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(group));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(meetingInviteRepository.findByMeetingIdAndRecipientEmail(10L, "bob@example.com"))
                .thenReturn(Optional.empty());
        when(meetingInviteRepository.saveAndFlush(any(MeetingInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.inviteBatch(1L, 10L, meetingBatch("bob"));

        InOrder order = inOrder(groupRepository, meetingRepository, meetingInviteRepository);
        order.verify(groupRepository).findByIdForUpdate(4L);
        order.verify(meetingRepository).findByIdForUpdate(10L);
        order.verify(meetingInviteRepository).saveAndFlush(any(MeetingInviteEntity.class));
    }

    @Test
    void inviteBatchMapsDuplicateInviteConstraintToConflict() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = verifiedUser(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice);

        when(authorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(meetingInviteRepository.findByMeetingIdAndRecipientEmail(10L, "bob@example.com"))
                .thenReturn(Optional.empty());
        when(meetingInviteRepository.saveAndFlush(any(MeetingInviteEntity.class)))
                .thenThrow(new DataIntegrityViolationException("ux_meeting_invites_email"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.inviteBatch(1L, 10L, meetingBatch("bob")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    void removeParticipantLocksMeeting() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        MeetingEntity meeting = meeting(10L, alice, alice, bob);

        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(authorization.canManage(1L, meeting)).thenReturn(true);

        service.removeParticipant(1L, 10L, 2L);

        verify(meetingRepository, never()).findById(anyLong());
        verify(meetingParticipantRepository).deleteByMeetingIdAndUserId(10L, 2L);
    }

    @Test
    void removeParticipantByNonManagerIsNotFound() {
        UserEntity alice = user(1L, "alice");
        MeetingEntity meeting = meeting(10L, alice, alice);

        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(authorization.canManage(3L, meeting)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.removeParticipant(3L, 10L, 1L));
        verify(meetingParticipantRepository, never()).deleteByMeetingIdAndUserId(anyLong(), anyLong());
    }
}
