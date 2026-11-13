package com.meetingplanner.domains.invite.service;

import com.meetingplanner.common.api.dto.CreateGroupShareLinkRequest;
import com.meetingplanner.common.api.dto.CreateMeetingShareLinkRequest;
import com.meetingplanner.common.api.dto.JoinRequestResponse;
import com.meetingplanner.common.api.dto.ShareLinkJoinResponse;
import com.meetingplanner.common.api.dto.ShareLinkPreviewResponse;
import com.meetingplanner.common.api.dto.ShareLinkResponse;
import com.meetingplanner.common.api.exceptions.InvalidTokenException;
import com.meetingplanner.common.api.exceptions.RateLimitExceededException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.ShareLinkJoinMode;
import com.meetingplanner.common.api.model.ShareLinkJoinOutcome;
import com.meetingplanner.domains.friend.service.BlockService;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.model.GroupShareLinkEntity;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.group.repository.GroupShareLinkRepository;
import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingParticipantEntity;
import com.meetingplanner.domains.meeting.model.MeetingShareLinkEntity;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.meeting.repository.MeetingShareLinkRepository;
import com.meetingplanner.domains.meeting.service.MeetingAuthorizationService;
import com.meetingplanner.domains.meeting.service.MeetingConflictService;
import com.meetingplanner.domains.meeting.service.MeetingService;
import com.meetingplanner.domains.request.service.JoinRequestService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import com.meetingplanner.domains.user.service.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareLinkServiceImplTest {

    private static final String TOKEN = "tok";

    @Mock
    private GroupShareLinkRepository groupShareLinkRepository;
    @Mock
    private MeetingShareLinkRepository meetingShareLinkRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupAuthorizationService groupAuthorization;
    @Mock
    private MeetingAuthorizationService meetingAuthorization;
    @Mock
    private MeetingConflictService conflictService;
    @Mock
    private MeetingService meetingService;
    @Mock
    private JoinRequestService joinRequestService;
    @Mock
    private BlockService blockService;
    @Mock
    private LoginAttemptService attemptCounter;

    @InjectMocks
    private ShareLinkServiceImpl service;

    private final UserEntity alice = user(1L, "alice");
    private final UserEntity bob = user(2L, "bob");

    private static UserEntity user(long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }

    private static GroupEntity group(boolean isPrivate, UserEntity owner) {
        GroupEntity group = new GroupEntity();
        group.setId(9L);
        group.setOwner(owner);
        group.setPrivate(isPrivate);
        return group;
    }

    private static GroupShareLinkEntity groupLink(GroupEntity group, UserEntity creator, ShareLinkJoinMode mode) {
        GroupShareLinkEntity link = new GroupShareLinkEntity();
        link.setId(50L);
        link.setGroup(group);
        link.setCreatedBy(creator);
        link.setToken(TOKEN);
        link.setJoinMode(mode);
        return link;
    }

    private static MeetingEntity meeting(UserEntity creator) {
        MeetingEntity meeting = new MeetingEntity();
        meeting.setId(30L);
        meeting.setTitle("Standup");
        meeting.setCreator(creator);
        meeting.setStartTime(OffsetDateTime.now().plusDays(1));
        return meeting;
    }

    private static MeetingShareLinkEntity meetingLink(MeetingEntity meeting, UserEntity creator) {
        MeetingShareLinkEntity link = new MeetingShareLinkEntity();
        link.setId(60L);
        link.setMeeting(meeting);
        link.setCreatedBy(creator);
        link.setToken(TOKEN);
        return link;
    }

    // Group link from alice (an admin/owner) that bob, a non-member, is opening.
    private GroupShareLinkEntity stubGroupJoin(GroupEntity group, ShareLinkJoinMode mode) {
        GroupShareLinkEntity link = groupLink(group, alice, mode);
        when(groupShareLinkRepository.findByToken(TOKEN)).thenReturn(Optional.of(link));
        when(groupRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isAdminOrOwner(1L, group)).thenReturn(true);
        return link;
    }

    private MeetingShareLinkEntity stubMeetingJoin(MeetingEntity meeting) {
        MeetingShareLinkEntity link = meetingLink(meeting, alice);
        when(meetingShareLinkRepository.findByToken(TOKEN)).thenReturn(Optional.of(link));
        when(meetingRepository.findById(30L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canManage(1L, meeting)).thenReturn(true);
        return link;
    }

    // ---- Management ----

    @Test
    void nonManagerCannotCreateGroupLink() {
        GroupEntity group = group(true, alice);
        when(groupRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isAdminOrOwner(2L, group)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.createGroupLink(2L, 9L,
                new CreateGroupShareLinkRequest(ShareLinkJoinMode.DIRECT, null, null)));
        verify(groupShareLinkRepository, never()).saveAndFlush(any());
    }

    @Test
    void createGroupLinkRevokesTheOldOneAndSavesSettings() {
        GroupEntity group = group(true, alice);
        when(groupRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isAdminOrOwner(1L, group)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        ShareLinkResponse response = service.createGroupLink(1L, 9L,
                new CreateGroupShareLinkRequest(ShareLinkJoinMode.REQUEST, 7, 5));

        verify(groupShareLinkRepository).revokeActive(eq(9L), any());
        ArgumentCaptor<GroupShareLinkEntity> saved = ArgumentCaptor.forClass(GroupShareLinkEntity.class);
        verify(groupShareLinkRepository).saveAndFlush(saved.capture());
        GroupShareLinkEntity link = saved.getValue();
        assertEquals(ShareLinkJoinMode.REQUEST, link.getJoinMode());
        assertEquals(5, link.getMaxUses());
        assertNotNull(link.getExpiresAt());
        assertEquals(64, link.getToken().length());
        assertTrue(response.url().endsWith("/join/" + link.getToken()));
        assertTrue(response.usable());
    }

    @Test
    void nonManagerCannotCreateMeetingLink() {
        MeetingEntity meeting = meeting(alice);
        when(meetingRepository.findById(30L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canManage(2L, meeting)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.createMeetingLink(2L, 30L, new CreateMeetingShareLinkRequest(null, null)));
        verify(meetingShareLinkRepository, never()).saveAndFlush(any());
    }

    @Test
    void cannotCreateLinkForEndedMeeting() {
        MeetingEntity meeting = meeting(alice);
        meeting.setStartTime(OffsetDateTime.now().minusDays(1));
        when(meetingRepository.findById(30L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(meeting));
        when(meetingAuthorization.canManage(1L, meeting)).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.createMeetingLink(1L, 30L, new CreateMeetingShareLinkRequest(null, null)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    // ---- Link validity ----

    @Test
    void unknownTokenIsInvalid() {
        assertThrows(InvalidTokenException.class, () -> service.join(2L, TOKEN));
    }

    @Test
    void revokedLinkIsInvalid() {
        GroupShareLinkEntity link = stubGroupJoin(group(true, alice), ShareLinkJoinMode.DIRECT);
        link.setRevokedAt(OffsetDateTime.now());

        assertThrows(InvalidTokenException.class, () -> service.join(2L, TOKEN));
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void expiredLinkIsInvalid() {
        GroupShareLinkEntity link = stubGroupJoin(group(true, alice), ShareLinkJoinMode.DIRECT);
        link.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

        InvalidTokenException e = assertThrows(InvalidTokenException.class, () -> service.join(2L, TOKEN));
        assertEquals("This link has expired", e.getMessage());
    }

    @Test
    void exhaustedLinkIsInvalid() {
        GroupShareLinkEntity link = stubGroupJoin(group(true, alice), ShareLinkJoinMode.DIRECT);
        link.setMaxUses(3);
        link.setUseCount(3);

        assertThrows(InvalidTokenException.class, () -> service.join(2L, TOKEN));
    }

    @Test
    void linkStopsWorkingWhenCreatorLosesAdminRights() {
        GroupEntity group = group(true, user(3L, "carol"));
        GroupShareLinkEntity link = groupLink(group, alice, ShareLinkJoinMode.DIRECT);
        when(groupShareLinkRepository.findByToken(TOKEN)).thenReturn(Optional.of(link));
        when(groupRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isAdminOrOwner(1L, group)).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> service.join(2L, TOKEN));
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void previewReportsEffectiveModeAndMembership() {
        GroupEntity group = group(false, alice);
        GroupShareLinkEntity link = groupLink(group, alice, ShareLinkJoinMode.REQUEST);
        when(groupShareLinkRepository.findByToken(TOKEN)).thenReturn(Optional.of(link));
        when(groupAuthorization.isAdminOrOwner(1L, group)).thenReturn(true);

        ShareLinkPreviewResponse anonymous = service.preview(null, TOKEN);
        assertEquals(ShareLinkJoinMode.DIRECT, anonymous.joinMode());
        assertNull(anonymous.viewerIsMember());

        when(groupAuthorization.isMember(2L, group)).thenReturn(true);
        assertTrue(service.preview(2L, TOKEN).viewerIsMember());
    }

    // ---- Group join ----

    @Test
    void directGroupLinkAddsMemberEvenToPrivateGroup() {
        GroupEntity group = group(true, alice);
        stubGroupJoin(group, ShareLinkJoinMode.DIRECT);
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(groupShareLinkRepository.incrementUse(50L)).thenReturn(1);

        ShareLinkJoinResponse response = service.join(2L, TOKEN);

        assertEquals(ShareLinkJoinOutcome.JOINED, response.outcome());
        ArgumentCaptor<GroupMemberEntity> member = ArgumentCaptor.forClass(GroupMemberEntity.class);
        verify(groupMemberRepository).save(member.capture());
        assertEquals(bob, member.getValue().getUser());
        assertFalse(member.getValue().isAdmin());
    }

    @Test
    void requestLinkOnPrivateGroupCreatesJoinRequest() {
        GroupEntity group = group(true, alice);
        stubGroupJoin(group, ShareLinkJoinMode.REQUEST);
        when(joinRequestService.requestGroupJoin(2L, 9L)).thenReturn(new JoinRequestResponse(
                77L, "GROUP", null, 2L, "bob", 9L, null, null, null, null, null));
        when(groupShareLinkRepository.incrementUse(50L)).thenReturn(1);

        ShareLinkJoinResponse response = service.join(2L, TOKEN);

        assertEquals(ShareLinkJoinOutcome.REQUESTED, response.outcome());
        assertEquals(77L, response.requestId());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void requestLinkOnPublicGroupJoinsDirectly() {
        GroupEntity group = group(false, alice);
        stubGroupJoin(group, ShareLinkJoinMode.REQUEST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(groupShareLinkRepository.incrementUse(50L)).thenReturn(1);

        assertEquals(ShareLinkJoinOutcome.JOINED, service.join(2L, TOKEN).outcome());
        verify(joinRequestService, never()).requestGroupJoin(anyLong(), anyLong());
    }

    @Test
    void existingMemberIsNotCountedAsAUse() {
        GroupEntity group = group(true, alice);
        stubGroupJoin(group, ShareLinkJoinMode.DIRECT);
        when(groupAuthorization.isMember(2L, group)).thenReturn(true);

        assertEquals(ShareLinkJoinOutcome.ALREADY_MEMBER, service.join(2L, TOKEN).outcome());
        verify(groupShareLinkRepository, never()).incrementUse(anyLong());
    }

    @Test
    void bannedUserCannotJoinGroup() {
        GroupEntity group = group(true, alice);
        stubGroupJoin(group, ShareLinkJoinMode.DIRECT);
        when(groupAuthorization.isBanned(2L, group)).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.join(2L, TOKEN));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void userBlockedWithCreatorCannotJoinGroup() {
        GroupEntity group = group(true, alice);
        stubGroupJoin(group, ShareLinkJoinMode.DIRECT);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "blocked"))
                .when(blockService).assertNotBlocked(eq(2L), eq(1L), anyString());

        assertThrows(ResponseStatusException.class, () -> service.join(2L, TOKEN));
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void joinFailsWhenLastUseIsTakenConcurrently() {
        GroupEntity group = group(true, alice);
        stubGroupJoin(group, ShareLinkJoinMode.DIRECT);
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(groupShareLinkRepository.incrementUse(50L)).thenReturn(0);

        assertThrows(InvalidTokenException.class, () -> service.join(2L, TOKEN));
    }

    // ---- Meeting join ----

    @Test
    void meetingLinkAddsParticipant() {
        MeetingEntity meeting = meeting(alice);
        stubMeetingJoin(meeting);
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(meetingShareLinkRepository.incrementUse(60L)).thenReturn(1);

        ShareLinkJoinResponse response = service.join(2L, TOKEN);

        assertEquals(ShareLinkJoinOutcome.JOINED, response.outcome());
        assertEquals(30L, response.meetingId());
        ArgumentCaptor<MeetingParticipantEntity> participant = ArgumentCaptor.forClass(MeetingParticipantEntity.class);
        verify(meetingParticipantRepository).saveAndFlush(participant.capture());
        assertEquals(bob, participant.getValue().getUser());
    }

    @Test
    void endedMeetingLinkIsInvalid() {
        MeetingEntity meeting = meeting(alice);
        meeting.setStartTime(OffsetDateTime.now().minusHours(2));
        stubMeetingJoin(meeting);

        assertThrows(InvalidTokenException.class, () -> service.join(2L, TOKEN));
        verify(meetingParticipantRepository, never()).saveAndFlush(any());
    }

    @Test
    void meetingJoinRejectsScheduleConflict() {
        MeetingEntity meeting = meeting(alice);
        stubMeetingJoin(meeting);
        when(conflictService.findScheduleConflicts(eq(2L), any(), any(), eq(30L))).thenReturn(List.of(meeting(bob)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.join(2L, TOKEN));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(meetingParticipantRepository, never()).saveAndFlush(any());
    }

    @Test
    void existingParticipantIsNotCountedAsAUse() {
        MeetingEntity meeting = meeting(alice);
        stubMeetingJoin(meeting);
        when(meetingAuthorization.isParticipantOrCreator(2L, meeting)).thenReturn(true);

        assertEquals(ShareLinkJoinOutcome.ALREADY_MEMBER, service.join(2L, TOKEN).outcome());
        verify(meetingShareLinkRepository, never()).incrementUse(anyLong());
    }

    // ---- Guest ----

    @Test
    void guestRequestIsQuietEvenWhenNoInviteIsSent() {
        MeetingEntity meeting = meeting(alice);
        MeetingShareLinkEntity link = meetingLink(meeting, alice);
        when(meetingShareLinkRepository.findByToken(TOKEN)).thenReturn(Optional.of(link));
        when(meetingAuthorization.canManage(1L, meeting)).thenReturn(true);
        when(meetingService.inviteFromShareLink(1L, 30L, "guest@example.com")).thenReturn(false);
        when(meetingShareLinkRepository.incrementUse(60L)).thenReturn(1);

        service.requestGuestInvite(TOKEN, "  guest@example.com ", "10.0.0.1");

        verify(meetingService).inviteFromShareLink(1L, 30L, "guest@example.com");
        verify(attemptCounter).incrementAttempts(ShareLinkServiceImpl.GUEST_EMAIL_PREFIX + "60:guest@example.com",
                ShareLinkServiceImpl.GUEST_EMAIL_WINDOW_SECONDS);
    }

    @Test
    void guestRequestIsRateLimitedPerIp() {
        MeetingEntity meeting = meeting(alice);
        MeetingShareLinkEntity link = meetingLink(meeting, alice);
        when(meetingShareLinkRepository.findByToken(TOKEN)).thenReturn(Optional.of(link));
        when(meetingAuthorization.canManage(1L, meeting)).thenReturn(true);
        when(attemptCounter.isBlocked(startsWith(ShareLinkServiceImpl.GUEST_IP_PREFIX), anyInt())).thenReturn(true);

        assertThrows(RateLimitExceededException.class,
                () -> service.requestGuestInvite(TOKEN, "guest@example.com", "10.0.0.1"));
        verify(meetingService, never()).inviteFromShareLink(anyLong(), anyLong(), anyString());
    }

    @Test
    void guestFormIsNotAvailableForGroupLinks() {
        when(groupShareLinkRepository.findByToken(TOKEN))
                .thenReturn(Optional.of(groupLink(group(true, alice), alice, ShareLinkJoinMode.DIRECT)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.requestGuestInvite(TOKEN, "guest@example.com", "10.0.0.1"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }
}
