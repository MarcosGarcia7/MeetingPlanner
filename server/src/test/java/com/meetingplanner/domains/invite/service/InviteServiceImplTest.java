package com.meetingplanner.domains.invite.service;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.InviteResponse;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.api.model.TargetKind;
import com.meetingplanner.domains.friend.service.BlockService;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import com.meetingplanner.domains.group.repository.GroupInviteRepository;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.group.service.GroupAuthorizationService;
import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.model.MeetingInviteEntity;
import com.meetingplanner.domains.meeting.repository.MeetingInviteRepository;
import com.meetingplanner.domains.meeting.repository.MeetingParticipantRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteServiceImplTest {

    @Mock
    private GroupInviteRepository groupInviteRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private MeetingInviteRepository meetingInviteRepository;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MeetingConflictService conflictService;
    @Mock
    private BlockService blockService;
    @Mock
    private GroupAuthorizationService groupAuthorization;

    @InjectMocks
    private InviteServiceImpl service;

    private static UserEntity user(long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }

    private static GroupEntity group(long id, UserEntity owner) {
        GroupEntity group = new GroupEntity();
        group.setId(id);
        group.setOwner(owner);
        return group;
    }

    private static GroupInviteEntity groupInvite(GroupEntity group, UserEntity sender, UserEntity recipient) {
        GroupInviteEntity invite = new GroupInviteEntity();
        invite.setId(5L);
        invite.setGroup(group);
        invite.setSender(sender);
        invite.setRecipient(recipient);
        invite.setRecipientEmail(recipient.getEmail());
        invite.setStatus(InviteStatus.PENDING);
        invite.setTimeSent(OffsetDateTime.now());
        return invite;
    }

    private static MeetingInviteEntity meetingInvite(MeetingEntity meeting, UserEntity sender, UserEntity recipient,
                                                     InviteStatus status) {
        MeetingInviteEntity invite = new MeetingInviteEntity();
        invite.setId(7L);
        invite.setMeeting(meeting);
        invite.setSender(sender);
        invite.setRecipient(recipient);
        invite.setRecipientEmail(recipient.getEmail());
        invite.setStatus(status);
        invite.setTimeSent(OffsetDateTime.now());
        return invite;
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

    @Test
    void bannedUserCannotAcceptGroupInviteById() {
        UserEntity sender = user(1L, "alice");
        UserEntity recipient = user(2L, "bob");
        GroupEntity group = group(9L, sender);
        GroupInviteEntity invite = groupInvite(group, sender, recipient);

        when(groupInviteRepository.findById(5L)).thenReturn(Optional.of(invite));
        when(groupRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isBanned(2L, group)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.acceptGroupInvite(2L, 5L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You are banned from this group", exception.getReason());
        verify(groupMemberRepository, never()).save(any());
        verify(groupInviteRepository, never()).transitionStatus(anyLong(), any(), any(), any());
    }

    @Test
    void bannedUserCannotAcceptGroupInviteByToken() {
        UserEntity sender = user(1L, "alice");
        UserEntity recipient = user(2L, "bob");
        GroupEntity group = group(9L, sender);
        GroupInviteEntity invite = groupInvite(group, sender, recipient);

        when(groupInviteRepository.findByTokenHash(anyString())).thenReturn(Optional.of(invite));
        when(groupRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isBanned(2L, group)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.acceptGroupInviteByToken("token"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You are banned from this group", exception.getReason());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void bannedUserCannotAcceptGroupMeetingInvite() {
        UserEntity sender = user(1L, "alice");
        UserEntity recipient = user(2L, "bob");
        GroupEntity group = group(9L, sender);
        MeetingInviteEntity invite = meetingInvite(meeting(3L, sender, group), sender, recipient, InviteStatus.PENDING);

        when(meetingInviteRepository.findById(7L)).thenReturn(Optional.of(invite));
        when(groupRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(group));
        when(groupAuthorization.isBanned(2L, group)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.acceptMeetingInvite(2L, 7L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You are banned from this group", exception.getReason());
        verify(meetingParticipantRepository, never()).saveAndFlush(any());
    }

    @Test
    void acceptGroupInviteClaimsInviteBeforeAddingMember() {
        UserEntity sender = user(1L, "alice");
        UserEntity recipient = user(2L, "bob");
        GroupEntity group = group(9L, sender);
        GroupInviteEntity invite = groupInvite(group, sender, recipient);

        when(groupInviteRepository.findById(5L)).thenReturn(Optional.of(invite));
        when(groupRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(group));
        when(groupInviteRepository.transitionStatus(eq(5L), eq(InviteStatus.PENDING),
                eq(InviteStatus.ACCEPTED), any(OffsetDateTime.class))).thenReturn(1);

        service.acceptGroupInvite(2L, 5L);

        assertEquals(InviteStatus.ACCEPTED, invite.getStatus());
        InOrder order = inOrder(groupInviteRepository, groupMemberRepository);
        order.verify(groupInviteRepository).transitionStatus(eq(5L), eq(InviteStatus.PENDING),
                eq(InviteStatus.ACCEPTED), any(OffsetDateTime.class));
        order.verify(groupMemberRepository).save(any());
    }

    @Test
    void acceptGroupInviteLosingRaceDoesNotAddMember() {
        UserEntity sender = user(1L, "alice");
        GroupEntity group = group(9L, sender);
        GroupInviteEntity invite = groupInvite(group, sender, user(2L, "bob"));

        when(groupInviteRepository.findById(5L)).thenReturn(Optional.of(invite));
        when(groupRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(group));
        when(groupInviteRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.acceptGroupInvite(2L, 5L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(InviteStatus.PENDING, invite.getStatus());
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void declineGroupInviteLosingRaceIsConflict() {
        UserEntity sender = user(1L, "alice");
        GroupInviteEntity invite = groupInvite(group(9L, sender), sender, user(2L, "bob"));

        when(groupInviteRepository.findById(5L)).thenReturn(Optional.of(invite));
        when(groupInviteRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.declineGroupInvite(2L, 5L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(InviteStatus.PENDING, invite.getStatus());
    }

    @Test
    void acceptMeetingInviteLosingRaceDoesNotAddParticipant() {
        UserEntity sender = user(1L, "alice");
        MeetingEntity meeting = meeting(3L, sender, null);
        MeetingInviteEntity invite = meetingInvite(meeting, sender, user(2L, "bob"), InviteStatus.PENDING);

        when(meetingInviteRepository.findById(7L)).thenReturn(Optional.of(invite));
        when(meetingRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(meeting));
        when(meetingInviteRepository.transitionStatus(anyLong(), any(), any(), any())).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.acceptMeetingInvite(2L, 7L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(meetingParticipantRepository, never()).saveAndFlush(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void leaveMeetingInviteLosingRaceKeepsParticipant() {
        UserEntity sender = user(1L, "alice");
        MeetingEntity meeting = meeting(3L, sender, null);
        MeetingInviteEntity invite = meetingInvite(meeting, sender, user(2L, "bob"), InviteStatus.ACCEPTED);

        when(meetingInviteRepository.findByTokenHash(anyString())).thenReturn(Optional.of(invite));
        when(meetingRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(meeting));
        when(meetingInviteRepository.transitionStatus(eq(7L), eq(InviteStatus.ACCEPTED),
                eq(InviteStatus.LEFT), any(OffsetDateTime.class))).thenReturn(0);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.leaveMeetingInviteByToken("token"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(meetingParticipantRepository, never()).deleteByMeetingIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void acceptGroupMeetingInviteLocksGroupThenMeetingThenInvite() {
        UserEntity sender = user(1L, "alice");
        UserEntity recipient = user(2L, "bob");
        GroupEntity group = group(9L, sender);
        MeetingEntity meeting = meeting(3L, sender, group);
        MeetingInviteEntity invite = meetingInvite(meeting, sender, recipient, InviteStatus.PENDING);

        when(meetingInviteRepository.findById(7L)).thenReturn(Optional.of(invite));
        when(groupRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(group));
        when(meetingRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(meeting));
        when(meetingInviteRepository.transitionStatus(eq(7L), eq(InviteStatus.PENDING),
                eq(InviteStatus.ACCEPTED), any(OffsetDateTime.class))).thenReturn(1);
        when(conflictService.findScheduleConflicts(eq(2L), any(), any(), eq(3L))).thenReturn(List.of());

        service.acceptMeetingInvite(2L, 7L);

        InOrder order = inOrder(groupRepository, meetingRepository, meetingInviteRepository,
                meetingParticipantRepository);
        order.verify(groupRepository).findByIdForUpdate(9L);
        order.verify(meetingRepository).findByIdForUpdate(3L);
        order.verify(meetingInviteRepository).transitionStatus(eq(7L), eq(InviteStatus.PENDING),
                eq(InviteStatus.ACCEPTED), any(OffsetDateTime.class));
        order.verify(meetingParticipantRepository).saveAndFlush(any());
    }

    @Test
    void acceptInviteForDeletedMeetingIsNotFound() {
        UserEntity sender = user(1L, "alice");
        MeetingInviteEntity invite = meetingInvite(meeting(3L, sender, null), sender, user(2L, "bob"),
                InviteStatus.PENDING);

        when(meetingInviteRepository.findById(7L)).thenReturn(Optional.of(invite));
        when(meetingRepository.findByIdForUpdate(3L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.acceptMeetingInvite(2L, 7L));
        verify(meetingInviteRepository, never()).transitionStatus(anyLong(), any(), any(), any());
        verify(meetingParticipantRepository, never()).saveAndFlush(any());
    }

    @Test
    void externalInviteAcceptanceAppendsToLockedMeeting() {
        UserEntity sender = user(1L, "alice");
        MeetingEntity meeting = meeting(3L, sender, null);
        MeetingInviteEntity invite = new MeetingInviteEntity();
        invite.setId(7L);
        invite.setMeeting(meeting);
        invite.setSender(sender);
        invite.setRecipientEmail("guest@example.com");
        invite.setStatus(InviteStatus.PENDING);

        when(meetingInviteRepository.findByTokenHash(anyString())).thenReturn(Optional.of(invite));
        when(meetingRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(meeting));
        when(meetingInviteRepository.transitionStatus(eq(7L), eq(InviteStatus.PENDING),
                eq(InviteStatus.ACCEPTED), any(OffsetDateTime.class))).thenReturn(1);

        service.acceptMeetingInviteByToken("token");

        assertEquals(List.of("guest@example.com"), meeting.getExternalParticipantNames());
        InOrder order = inOrder(meetingRepository);
        order.verify(meetingRepository).findByIdForUpdate(3L);
        order.verify(meetingRepository).save(meeting);
    }

    @Test
    void listMinePassesStatusFilterAndLabelsInvites() {
        UserEntity owner = user(1L, "owner");
        UserEntity recipient = user(2L, "recipient");
        GroupEntity group = group(3L, owner);
        group.setGroupName("Book club");
        MeetingEntity meeting = meeting(4L, owner, null);
        GroupInviteEntity groupInvite = groupInvite(group, owner, recipient);
        MeetingInviteEntity meetingInvite = meetingInvite(meeting, owner, recipient, InviteStatus.PENDING);
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(groupInviteRepository.listForRecipient(eq(2L), eq("recipient@example.com"), eq(InviteStatus.PENDING),
                eq("GROUP"), any(), any(), any(), any())).thenReturn(List.of(groupInvite));
        when(meetingInviteRepository.listForRecipient(eq(2L), eq("recipient@example.com"), eq(InviteStatus.PENDING),
                eq("MEETING"), any(), any(), any(), any())).thenReturn(List.of(meetingInvite));

        CursorPage<InviteResponse> page = service.listMine(2L, InviteStatus.PENDING, null, null, 20);

        assertEquals(2, page.items().size());
        InviteResponse groupItem = page.items().stream().filter(i -> i.kind().equals("GROUP")).findFirst().orElseThrow();
        InviteResponse meetingItem = page.items().stream().filter(i -> i.kind().equals("MEETING")).findFirst().orElseThrow();
        assertEquals("Book club", groupItem.groupName());
        assertEquals("Standup", meetingItem.meetingTitle());
        assertNull(page.nextCursor());
    }

    @Test
    void listMineWithGroupKindSkipsMeetingInvites() {
        UserEntity recipient = user(2L, "recipient");
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(groupInviteRepository.listForRecipient(eq(2L), eq("recipient@example.com"), eq(null),
                eq("GROUP"), any(), any(), any(), any())).thenReturn(List.of());

        service.listMine(2L, null, TargetKind.GROUP, null, 20);

        verifyNoInteractions(meetingInviteRepository);
    }

    @Test
    void listMineWithMeetingKindSkipsGroupInvites() {
        UserEntity recipient = user(2L, "recipient");
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(meetingInviteRepository.listForRecipient(eq(2L), eq("recipient@example.com"), eq(null),
                eq("MEETING"), any(), any(), any(), any())).thenReturn(List.of());

        service.listMine(2L, null, TargetKind.MEETING, null, 20);

        verifyNoInteractions(groupInviteRepository);
    }
}
