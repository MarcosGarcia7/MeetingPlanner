package com.meetingplanner.domains.request.service;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.JoinRequestResponse;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.RequestStatus;
import com.meetingplanner.common.api.model.TargetKind;
import com.meetingplanner.domains.friend.service.BlockService;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupJoinRequestEntity;
import com.meetingplanner.domains.group.repository.GroupJoinRequestRepository;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingJoinRequestEntity;
import com.meetingplanner.domains.meeting.repository.MeetingJoinRequestRepository;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.meeting.service.MeetingAuthorizationService;
import com.meetingplanner.domains.meeting.service.MeetingConflictService;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinRequestServiceImplTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupJoinRequestRepository groupJoinRequestRepository;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;
    @Mock
    private MeetingJoinRequestRepository meetingJoinRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupAuthorizationService groupAuthorization;
    @Mock
    private MeetingAuthorizationService meetingAuthorization;
    @Mock
    private MeetingConflictService conflictService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BlockService blockService;

    @InjectMocks
    private JoinRequestServiceImpl service;

    private static UserEntity user(long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    @Test
    void blockedUserCannotRequestMeetingJoin() {
        UserEntity creator = user(1L, "alice");
        GroupEntity group = new GroupEntity();
        group.setId(5L);
        group.setPrivate(false);
        group.setOwner(creator);
        MeetingEntity meeting = new MeetingEntity();
        meeting.setId(10L);
        meeting.setCreator(creator);
        meeting.setGroup(group);
        meeting.setPrivate(false);
        meeting.setStartTime(OffsetDateTime.now().plusDays(1));

        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canView(2L, meeting)).thenReturn(true);
        when(meetingAuthorization.isParticipantOrCreator(2L, meeting)).thenReturn(false);
        when(blockService.isBlocked(2L, 1L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requestMeetingJoin(2L, 10L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(meetingJoinRequestRepository, never()).saveAndFlush(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void blockedUserCanStillRequestGroupJoin() {
        UserEntity owner = user(1L, "alice");
        UserEntity requester = user(2L, "bob");
        GroupEntity group = new GroupEntity();
        group.setId(5L);
        group.setGroupName("Team");
        group.setPrivate(true);
        group.setOwner(owner);

        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isOwner(2L, group)).thenReturn(false);
        when(groupAuthorization.isMember(2L, group)).thenReturn(false);
        when(groupJoinRequestRepository.existsByGroupIdAndRequesterIdAndStatus(
                eq(5L), eq(2L), any())).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));
        when(groupMemberRepository.findByGroupId(5L)).thenReturn(List.of());

        service.requestGroupJoin(2L, 5L);

        verify(groupJoinRequestRepository).saveAndFlush(any());
        verify(blockService, never()).isBlocked(anyLong(), anyLong());
    }

    @Test
    void bannedUserCannotRequestGroupJoin() {
        UserEntity owner = user(1L, "alice");
        GroupEntity group = new GroupEntity();
        group.setId(5L);
        group.setPrivate(true);
        group.setOwner(owner);

        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isBanned(2L, group)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requestGroupJoin(2L, 5L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You are banned from this group", exception.getReason());
        verify(groupJoinRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void bannedUserCannotRequestMeetingJoin() {
        UserEntity creator = user(1L, "alice");
        GroupEntity group = new GroupEntity();
        group.setId(5L);
        group.setPrivate(false);
        group.setOwner(creator);
        MeetingEntity meeting = new MeetingEntity();
        meeting.setId(10L);
        meeting.setCreator(creator);
        meeting.setGroup(group);
        meeting.setPrivate(false);
        meeting.setStartTime(OffsetDateTime.now().plusDays(1));

        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canView(2L, meeting)).thenReturn(true);
        when(groupAuthorization.isBanned(2L, group)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requestMeetingJoin(2L, 10L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(meetingJoinRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void bannedRequesterCannotBeApproved() {
        UserEntity owner = user(1L, "alice");
        UserEntity requester = user(2L, "bob");
        GroupEntity group = new GroupEntity();
        group.setId(5L);
        group.setOwner(owner);
        GroupJoinRequestEntity request = new GroupJoinRequestEntity();
        request.setId(11L);
        request.setGroup(group);
        request.setRequester(requester);
        request.setStatus(RequestStatus.PENDING);

        when(groupJoinRequestRepository.findById(11L)).thenReturn(Optional.of(request));
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isAdminOrOwner(1L, group)).thenReturn(true);
        when(groupAuthorization.isBanned(2L, group)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.approveGroup(1L, 11L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("The requester is banned from this group", exception.getReason());
        verify(groupMemberRepository, never()).save(any());
        verify(groupJoinRequestRepository, never()).transitionStatus(anyLong(), any(), any(), any());
    }

    @Test
    void bannedRequesterCannotBeApprovedForGroupMeeting() {
        UserEntity creator = user(1L, "alice");
        UserEntity requester = user(2L, "bob");
        GroupEntity group = group(5L, creator);
        MeetingEntity meeting = meeting(10L, creator, group);
        MeetingJoinRequestEntity request = meetingRequest(12L, meeting, requester);

        when(meetingJoinRequestRepository.findById(12L)).thenReturn(Optional.of(request));
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canManage(1L, meeting)).thenReturn(true);
        when(groupAuthorization.isBanned(2L, group)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.approveMeeting(1L, 12L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("The requester is banned from this group", exception.getReason());
        verify(meetingParticipantRepository, never()).save(any());
        verify(meetingJoinRequestRepository, never()).transitionStatus(anyLong(), any(), any(), any());
    }

    @Test
    void approveGroupClaimsRequestBeforeAddingMember() {
        UserEntity owner = user(1L, "alice");
        UserEntity requester = user(2L, "bob");
        GroupEntity group = group(5L, owner);
        GroupJoinRequestEntity request = groupRequest(11L, group, requester);

        when(groupJoinRequestRepository.findById(11L)).thenReturn(Optional.of(request));
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isAdminOrOwner(1L, group)).thenReturn(true);
        when(groupJoinRequestRepository.transitionStatus(eq(11L), eq(RequestStatus.PENDING),
                eq(RequestStatus.APPROVED), any(OffsetDateTime.class))).thenReturn(1);

        service.approveGroup(1L, 11L);

        assertEquals(RequestStatus.APPROVED, request.getStatus());
        InOrder order = inOrder(groupJoinRequestRepository, groupMemberRepository);
        order.verify(groupJoinRequestRepository).transitionStatus(eq(11L), eq(RequestStatus.PENDING),
                eq(RequestStatus.APPROVED), any(OffsetDateTime.class));
        order.verify(groupMemberRepository).save(any());
        verify(notificationService).push(eq(2L), eq("GROUP_JOIN_REQUEST_APPROVED"), anyString(),
                eq("GROUP_JOIN_REQUEST_APPROVED"), anyMap());
    }

    @Test
    void approveGroupLosingRaceDoesNotAddMember() {
        UserEntity owner = user(1L, "alice");
        GroupEntity group = group(5L, owner);
        GroupJoinRequestEntity request = groupRequest(11L, group, user(2L, "bob"));

        when(groupJoinRequestRepository.findById(11L)).thenReturn(Optional.of(request));
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isAdminOrOwner(1L, group)).thenReturn(true);
        when(groupJoinRequestRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.approveGroup(1L, 11L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(groupMemberRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void declineGroupLosingRaceDoesNotNotify() {
        UserEntity owner = user(1L, "alice");
        GroupEntity group = group(5L, owner);
        GroupJoinRequestEntity request = groupRequest(11L, group, user(2L, "bob"));

        when(groupJoinRequestRepository.findById(11L)).thenReturn(Optional.of(request));
        when(groupAuthorization.isAdminOrOwner(1L, group)).thenReturn(true);
        when(groupJoinRequestRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.declineGroup(1L, 11L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    void cancelGroupLosingRaceIsConflict() {
        GroupJoinRequestEntity request = groupRequest(11L, group(5L, user(1L, "alice")), user(2L, "bob"));

        when(groupJoinRequestRepository.findByIdAndRequesterId(11L, 2L)).thenReturn(Optional.of(request));
        when(groupJoinRequestRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.cancelGroup(2L, 11L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(RequestStatus.PENDING, request.getStatus());
    }

    @Test
    void approveMeetingLosingRaceDoesNotAddParticipant() {
        UserEntity creator = user(1L, "alice");
        MeetingEntity meeting = meeting(10L, creator, null);
        MeetingJoinRequestEntity request = meetingRequest(12L, meeting, user(2L, "bob"));

        when(meetingJoinRequestRepository.findById(12L)).thenReturn(Optional.of(request));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingJoinRequestRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.approveMeeting(1L, 12L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(meetingParticipantRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void cancelMeetingLosingRaceIsConflict() {
        MeetingJoinRequestEntity request = meetingRequest(12L, meeting(10L, user(1L, "alice"), null), user(2L, "bob"));

        when(meetingJoinRequestRepository.findByIdAndRequesterId(12L, 2L)).thenReturn(Optional.of(request));
        when(meetingJoinRequestRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.cancelMeeting(2L, 12L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void concurrentDuplicateGroupJoinRequestIsConflict() {
        UserEntity owner = user(1L, "alice");
        GroupEntity group = group(5L, owner);
        group.setPrivate(true);

        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(groupJoinRequestRepository.existsByGroupIdAndRequesterIdAndStatus(5L, 2L, RequestStatus.PENDING))
                .thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "bob")));
        when(groupJoinRequestRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("ux_group_join_requests_pending"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requestGroupJoin(2L, 5L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("A join request is already pending", exception.getReason());
        verifyNoInteractions(notificationService);
    }

    @Test
    void requestGroupJoinForMissingGroupIsNotFound() {
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.requestGroupJoin(2L, 5L));
        verify(groupJoinRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void requestMeetingJoinLocksGroupBeforeMeetingAndMapsDuplicateToConflict() {
        UserEntity creator = user(1L, "alice");
        GroupEntity group = group(5L, creator);
        MeetingEntity meeting = meeting(10L, creator, group);
        group.setPrivate(false);
        meeting.setPrivate(false);

        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canView(2L, meeting)).thenReturn(true);
        when(meetingJoinRequestRepository.existsByMeetingIdAndRequesterIdAndStatus(10L, 2L, RequestStatus.PENDING))
                .thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "bob")));
        when(meetingJoinRequestRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("ux_meeting_join_requests_pending"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requestMeetingJoin(2L, 10L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        InOrder order = inOrder(groupRepository, meetingRepository);
        order.verify(groupRepository).findByIdForUpdate(5L);
        order.verify(meetingRepository).findByIdForUpdate(10L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void requestMeetingJoinRejectsEndedMeeting() {
        UserEntity creator = user(1L, "alice");
        GroupEntity group = group(5L, creator);
        MeetingEntity meeting = meeting(10L, creator, group);
        group.setPrivate(false);
        meeting.setStartTime(OffsetDateTime.now().minusHours(2));
        meeting.setEndTime(OffsetDateTime.now().minusHours(1));

        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canView(2L, meeting)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.requestMeetingJoin(2L, 10L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(meetingJoinRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void requestMeetingJoinForMeetingViewerCannotSeeIsNotFound() {
        UserEntity creator = user(1L, "alice");
        GroupEntity group = group(5L, creator);
        MeetingEntity meeting = meeting(10L, creator, group);
        meeting.setPrivate(true);

        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canView(2L, meeting)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.requestMeetingJoin(2L, 10L));
        verify(meetingJoinRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void requestMeetingJoinAllowedForMemberOfPrivateGroup() {
        UserEntity creator = user(1L, "alice");
        GroupEntity group = group(5L, creator);
        group.setPrivate(true);
        MeetingEntity meeting = meeting(10L, creator, group);

        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canView(2L, meeting)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "bob")));

        JoinRequestResponse response = service.requestMeetingJoin(2L, 10L);

        assertEquals(10L, response.meetingId());
        verify(meetingJoinRequestRepository).saveAndFlush(any());
        verify(notificationService).push(eq(1L), eq("MEETING_JOIN_REQUEST"), anyString(), eq("MEETING_JOIN_REQUEST"),
                anyMap());
    }

    @Test
    void requestMeetingJoinForNonGroupMeetingIsNotFound() {
        MeetingEntity meeting = meeting(10L, user(1L, "alice"), null);
        when(meetingRepository.findById(10L)).thenReturn(Optional.of(meeting));

        assertThrows(ResourceNotFoundException.class, () -> service.requestMeetingJoin(2L, 10L));
        verifyNoInteractions(groupRepository);
    }

    @Test
    void approveMeetingLocksGroupBeforeMeeting() {
        UserEntity creator = user(1L, "alice");
        UserEntity requester = user(2L, "bob");
        GroupEntity group = group(5L, creator);
        MeetingEntity meeting = meeting(10L, creator, group);
        MeetingJoinRequestEntity request = meetingRequest(12L, meeting, requester);

        when(meetingJoinRequestRepository.findById(12L)).thenReturn(Optional.of(request));
        when(groupRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(meetingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canManage(1L, meeting)).thenReturn(true);
        when(conflictService.findScheduleConflicts(eq(2L), any(), any(), eq(10L))).thenReturn(List.of());
        when(meetingJoinRequestRepository.transitionStatus(eq(12L), eq(RequestStatus.PENDING),
                eq(RequestStatus.APPROVED), any(OffsetDateTime.class))).thenReturn(1);

        service.approveMeeting(1L, 12L);

        InOrder order = inOrder(groupRepository, meetingRepository, meetingJoinRequestRepository,
                meetingParticipantRepository);
        order.verify(groupRepository).findByIdForUpdate(5L);
        order.verify(meetingRepository).findByIdForUpdate(10L);
        order.verify(meetingJoinRequestRepository).transitionStatus(eq(12L), eq(RequestStatus.PENDING),
                eq(RequestStatus.APPROVED), any(OffsetDateTime.class));
        order.verify(meetingParticipantRepository).saveAndFlush(any());
    }

    @Test
    void listMinePassesStatusFilterToBothSources() {
        UserEntity owner = user(1L, "alice");
        UserEntity requester = user(2L, "bob");
        GroupEntity group = group(3L, owner);
        MeetingEntity meeting = meeting(4L, owner, null);
        when(groupJoinRequestRepository.listForRequester(eq(2L), eq(RequestStatus.PENDING), eq("GROUP"),
                any(), any(), any(), any())).thenReturn(List.of(groupRequest(10L, group, requester)));
        when(meetingJoinRequestRepository.listForRequester(eq(2L), eq(RequestStatus.PENDING), eq("MEETING"),
                any(), any(), any(), any())).thenReturn(List.of(meetingRequest(11L, meeting, requester)));

        CursorPage<JoinRequestResponse> page = service.listMine(2L, RequestStatus.PENDING, null, null, 20);

        assertEquals(2, page.items().size());
        JoinRequestResponse groupItem = page.items().stream().filter(r -> r.kind().equals("GROUP")).findFirst().orElseThrow();
        assertEquals("Team", groupItem.groupName());
        JoinRequestResponse meetingItem = page.items().stream().filter(r -> r.kind().equals("MEETING")).findFirst().orElseThrow();
        assertEquals(4L, meetingItem.meetingId());
        assertEquals("Standup", meetingItem.meetingTitle());
        assertNull(page.nextCursor());
    }

    @Test
    void listMineWithGroupKindSkipsMeetingQuery() {
        when(groupJoinRequestRepository.listForRequester(eq(2L), eq(null), eq("GROUP"),
                any(), any(), any(), any())).thenReturn(List.of());

        service.listMine(2L, null, TargetKind.GROUP, null, 20);

        verifyNoInteractions(meetingJoinRequestRepository);
    }

    @Test
    void listMineWithMeetingKindSkipsGroupQuery() {
        when(meetingJoinRequestRepository.listForRequester(eq(2L), eq(null), eq("MEETING"),
                any(), any(), any(), any())).thenReturn(List.of());

        service.listMine(2L, null, TargetKind.MEETING, null, 20);

        verifyNoInteractions(groupJoinRequestRepository);
    }

    @Test
    void listIncomingMergesManagedGroupAndMeetingRequests() {
        UserEntity owner = user(1L, "alice");
        UserEntity requester = user(2L, "bob");
        GroupEntity group = group(3L, owner);
        MeetingEntity meeting = meeting(4L, owner, null);
        when(groupJoinRequestRepository.listForManager(eq(1L), eq(RequestStatus.PENDING), eq("GROUP"),
                any(), any(), any(), any())).thenReturn(List.of(groupRequest(10L, group, requester)));
        when(meetingJoinRequestRepository.listForManager(eq(1L), eq(RequestStatus.PENDING), eq("MEETING"),
                any(), any(), any(), any())).thenReturn(List.of(meetingRequest(11L, meeting, requester)));

        CursorPage<JoinRequestResponse> page = service.listIncoming(1L, RequestStatus.PENDING, null, null, 20);

        assertEquals(2, page.items().size());
        assertEquals("bob", page.items().getFirst().requesterUsername());
        assertNull(page.nextCursor());
        verify(groupJoinRequestRepository, never()).listForRequester(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void listIncomingWithGroupKindSkipsMeetingQuery() {
        when(groupJoinRequestRepository.listForManager(eq(1L), eq(null), eq("GROUP"),
                any(), any(), any(), any())).thenReturn(List.of());

        service.listIncoming(1L, null, TargetKind.GROUP, null, 20);

        verifyNoInteractions(meetingJoinRequestRepository);
    }

    @Test
    void listForMeetingPassesStatusFilter() {
        UserEntity owner = user(1L, "alice");
        MeetingEntity meeting = meeting(4L, owner, null);
        when(meetingRepository.findById(4L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingJoinRequestRepository.listForMeeting(eq(4L), eq(1L), eq(RequestStatus.PENDING), any(), any(), any()))
                .thenReturn(List.of(meetingRequest(11L, meeting, user(2L, "bob"))));

        CursorPage<JoinRequestResponse> page = service.listForMeeting(1L, 4L, RequestStatus.PENDING, null, 20);

        assertEquals(1, page.items().size());
        assertEquals("bob", page.items().getFirst().requesterUsername());
    }

    @Test
    void listForMeetingHidesMeetingFromNonManagers() {
        MeetingEntity meeting = meeting(4L, user(1L, "alice"), null);
        when(meetingRepository.findById(4L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canManage(2L, meeting)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.listForMeeting(2L, 4L, RequestStatus.PENDING, null, 20));
        verifyNoInteractions(meetingJoinRequestRepository);
    }

    @Test
    void listForGroupPassesStatusFilter() {
        UserEntity owner = user(1L, "alice");
        GroupEntity group = group(3L, owner);
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isAdminOrOwner(1L, group)).thenReturn(true);
        when(groupJoinRequestRepository.listForGroup(eq(3L), eq(RequestStatus.PENDING), any(), any(), any()))
                .thenReturn(List.of(groupRequest(10L, group, user(2L, "bob"))));

        CursorPage<JoinRequestResponse> page = service.listForGroup(1L, 3L, RequestStatus.PENDING, null, 20);

        assertEquals(1, page.items().size());
        assertEquals("bob", page.items().getFirst().requesterUsername());
    }

    @Test
    void listForGroupHidesGroupFromNonAdmins() {
        GroupEntity group = group(3L, user(1L, "alice"));
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isAdminOrOwner(2L, group)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.listForGroup(2L, 3L, RequestStatus.PENDING, null, 20));
        verifyNoInteractions(groupJoinRequestRepository);
    }

    private static GroupEntity group(long id, UserEntity owner) {
        GroupEntity group = new GroupEntity();
        group.setId(id);
        group.setGroupName("Team");
        group.setOwner(owner);
        return group;
    }

    private static MeetingEntity meeting(long id, UserEntity creator, GroupEntity group) {
        MeetingEntity meeting = new MeetingEntity();
        meeting.setId(id);
        meeting.setTitle("Standup");
        meeting.setCreator(creator);
        meeting.setGroup(group);
        meeting.setStartTime(OffsetDateTime.now().plusDays(1));
        return meeting;
    }

    private static GroupJoinRequestEntity groupRequest(long id, GroupEntity group, UserEntity requester) {
        GroupJoinRequestEntity request = new GroupJoinRequestEntity();
        request.setId(id);
        request.setGroup(group);
        request.setRequester(requester);
        request.setStatus(RequestStatus.PENDING);
        return request;
    }

    private static MeetingJoinRequestEntity meetingRequest(long id, MeetingEntity meeting, UserEntity requester) {
        MeetingJoinRequestEntity request = new MeetingJoinRequestEntity();
        request.setId(id);
        request.setMeeting(meeting);
        request.setRequester(requester);
        request.setStatus(RequestStatus.PENDING);
        return request;
    }
}
