package com.meetingplanner.domains.group.service;

import com.meetingplanner.common.api.dto.GroupCreateRequest;
import com.meetingplanner.common.api.dto.GroupInviteBatchRequest;
import com.meetingplanner.common.api.dto.GroupResponse;
import com.meetingplanner.common.api.dto.InviteBatchResponse;
import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.common.api.exceptions.GroupNameTakenException;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.repository.GroupInviteRepository;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    @Mock
    private GroupAuthorizationService authorization;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupInviteRepository groupInviteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private GroupServiceImpl service;

    private static UserEntity user(long id, String username, boolean verified) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        if (verified) {
            user.setEmailVerifiedAt(OffsetDateTime.now());
        }
        return user;
    }

    private static GroupEntity group(long id) {
        GroupEntity group = new GroupEntity();
        group.setId(id);
        group.setGroupName("Team");
        return group;
    }

    private static GroupInviteBatchRequest batch(String... usernames) {
        return new GroupInviteBatchRequest(
                Arrays.stream(usernames).map(MemberUsernameRequest::new).toList());
    }

    private static GroupMemberEntity member(GroupEntity group, UserEntity user, boolean admin) {
        GroupMemberEntity member = new GroupMemberEntity();
        member.setGroup(group);
        member.setUser(user);
        member.setAdmin(admin);
        return member;
    }

    private GroupEntity prepareAdmin(long groupId, UserEntity actor) {
        GroupEntity group = group(groupId);
        GroupMemberEntity actorMember = member(group, actor, true);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actor.getId()))
                .thenReturn(Optional.of(actorMember));
        when(authorization.isAdminOrOwner(actorMember, group)).thenReturn(true);
        return group;
    }

    @Test
    void nonAdminCannotInvite() {
        UserEntity actor = user(9L, "dave", true);
        GroupEntity group = group(1L);
        GroupMemberEntity actorMember = member(group, actor, false);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 9L))
                .thenReturn(Optional.of(actorMember));
        when(authorization.isAdminOrOwner(actorMember, group)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.inviteBatch(9L, 1L, batch("bob")));

        verifyNoInteractions(userRepository, notificationService);
    }

    @Test
    void duplicateUsernameIsReportedOnce() {
        UserEntity actor = user(1L, "alice", true);
        UserEntity bob = user(2L, "bob", true);
        prepareAdmin(1L, actor);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L)).thenReturn(false);
        when(groupInviteRepository.findByGroupIdAndRecipientEmail(1L, "bob@example.com"))
                .thenReturn(Optional.empty());
        when(groupInviteRepository.save(any(GroupInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InviteBatchResponse response = service.inviteBatch(1L, 1L,
                batch("bob", " BOB "));

        assertEquals(1, response.invited().size());
        assertEquals(1, response.failed().size());
        assertEquals("Duplicate recipient in request", response.failed().getFirst().reason());
        verify(groupInviteRepository, times(1))
                .findByGroupIdAndRecipientEmail(1L, "bob@example.com");
        verify(notificationService).queueEmailToForUser(eq(bob), eq("bob@example.com"), eq("GROUP_INVITE"), anyMap(), eq("group_invites"));
    }

    @Test
    void existingMemberIsRejected() {
        UserEntity actor = user(1L, "alice", true);
        UserEntity bob = user(2L, "bob", true);
        UserEntity carol = user(3L, "carol", true);
        prepareAdmin(1L, actor);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(carol));
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 3L)).thenReturn(false);
        when(groupInviteRepository.findByGroupIdAndRecipientEmail(1L, "carol@example.com"))
                .thenReturn(Optional.empty());
        when(groupInviteRepository.save(any(GroupInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InviteBatchResponse response = service.inviteBatch(1L, 1L, batch("bob", "carol"));

        assertEquals(1, response.failed().size());
        assertEquals("User is already a member of this group", response.failed().getFirst().reason());
        assertEquals(1, response.invited().size());
        assertEquals(3L, response.invited().getFirst().recipientId());
        verify(groupInviteRepository, never())
                .findByGroupIdAndRecipientEmail(1L, "bob@example.com");
    }

    @Test
    void selfInviteIsRejectedBeforeEmailVerification() {
        UserEntity actor = user(1L, "alice", false);
        UserEntity bob = user(2L, "bob", true);
        prepareAdmin(1L, actor);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(actor));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L)).thenReturn(false);
        when(groupInviteRepository.findByGroupIdAndRecipientEmail(1L, "bob@example.com"))
                .thenReturn(Optional.empty());
        when(groupInviteRepository.save(any(GroupInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InviteBatchResponse response = service.inviteBatch(1L, 1L, batch("alice", "bob"));

        assertEquals(1, response.failed().size());
        assertEquals("You cannot invite yourself", response.failed().getFirst().reason());
    }

    @Test
    void unverifiedRecipientIsRejected() {
        UserEntity actor = user(1L, "alice", true);
        UserEntity bob = user(2L, "bob", false);
        UserEntity carol = user(3L, "carol", true);
        prepareAdmin(1L, actor);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(carol));
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L)).thenReturn(false);
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 3L)).thenReturn(false);
        when(groupInviteRepository.findByGroupIdAndRecipientEmail(1L, "carol@example.com"))
                .thenReturn(Optional.empty());
        when(groupInviteRepository.save(any(GroupInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InviteBatchResponse response = service.inviteBatch(1L, 1L, batch("bob", "carol"));

        assertEquals(1, response.failed().size());
        assertEquals("Email address is not verified", response.failed().getFirst().reason());
    }

    @Test
    void partialFailureReturnsInvitedAndFailed() {
        UserEntity actor = user(1L, "alice", true);
        UserEntity bob = user(2L, "bob", true);
        prepareAdmin(1L, actor);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L)).thenReturn(false);
        when(groupInviteRepository.findByGroupIdAndRecipientEmail(1L, "bob@example.com"))
                .thenReturn(Optional.empty());
        when(groupInviteRepository.save(any(GroupInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InviteBatchResponse response = service.inviteBatch(1L, 1L, batch("ghost", "bob"));

        assertEquals(1, response.invited().size());
        assertEquals(1, response.failed().size());
        assertEquals("ghost", response.failed().getFirst().username());
        assertEquals("User not found", response.failed().getFirst().reason());
    }

    @Test
    void allRecipientsFailingReturnsBadRequest() {
        UserEntity actor = user(1L, "alice", true);
        prepareAdmin(1L, actor);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.inviteBatch(1L, 1L, batch("ghost")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    void reinviteRefreshesTimeSentAndToken() {
        UserEntity actor = user(1L, "alice", true);
        UserEntity bob = user(2L, "bob", true);
        prepareAdmin(1L, actor);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L)).thenReturn(false);

        GroupInviteEntity existing = new GroupInviteEntity();
        existing.setId(5L);
        existing.setStatus(InviteStatus.DECLINED);
        OffsetDateTime staleSentAt = OffsetDateTime.now().minusDays(3);
        existing.setTimeSent(staleSentAt);
        when(groupInviteRepository.findByGroupIdAndRecipientEmail(1L, "bob@example.com"))
                .thenReturn(Optional.of(existing));
        when(groupInviteRepository.save(any(GroupInviteEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InviteBatchResponse response = service.inviteBatch(1L, 1L, batch("bob"));

        assertEquals(1, response.invited().size());
        assertEquals(InviteStatus.PENDING, existing.getStatus());
        assertNull(existing.getRespondedAt());
        assertTrue(existing.getTimeSent().isAfter(staleSentAt));
        assertNotNull(existing.getTokenHash());
        verify(notificationService).queueEmailToForUser(eq(bob), eq("bob@example.com"), eq("GROUP_INVITE"), anyMap(), eq("group_invites"));
    }

    @Test
    void persistenceFailuresPropagate() {
        UserEntity actor = user(1L, "alice", true);
        UserEntity bob = user(2L, "bob", true);
        prepareAdmin(1L, actor);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L)).thenReturn(false);
        when(groupInviteRepository.findByGroupIdAndRecipientEmail(1L, "bob@example.com"))
                .thenReturn(Optional.empty());
        when(groupInviteRepository.save(any(GroupInviteEntity.class)))
                .thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class,
                () -> service.inviteBatch(1L, 1L, batch("bob")));
    }

    @Test
    void createPersistsGroupWithOwnerAsAdminMember() {
        UserEntity owner = user(1L, "alice", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(groupRepository.existsByGroupName("Team")).thenReturn(false);
        when(groupRepository.save(any(GroupEntity.class))).thenAnswer(inv -> {
            GroupEntity group = inv.getArgument(0);
            group.setId(10L);
            return group;
        });
        when(groupMemberRepository.countByGroupId(10L)).thenReturn(1L);

        GroupResponse response = service.create(1L, new GroupCreateRequest("  Team  ", null));

        assertEquals(10L, response.id());
        assertEquals("Team", response.groupName());
        assertEquals(1L, response.ownerId());
        assertTrue(response.isPrivate());
        assertEquals(1, response.memberCount());
        assertEquals(1, response.members().size());
        assertEquals(1L, response.members().getFirst().userId());
        assertEquals("alice", response.members().getFirst().username());
        assertTrue(response.members().getFirst().isAdmin());

        ArgumentCaptor<GroupEntity> captor = ArgumentCaptor.forClass(GroupEntity.class);
        verify(groupRepository).save(captor.capture());
        GroupEntity saved = captor.getValue();
        assertEquals("Team", saved.getGroupName());
        assertEquals(owner, saved.getOwner());
        assertTrue(saved.isPrivate());
        assertEquals(1, saved.getMembers().size());
        GroupMemberEntity member = saved.getMembers().iterator().next();
        assertEquals(owner, member.getUser());
        assertTrue(member.isAdmin());
    }

    @Test
    void createWithExplicitPublicVisibilityIsPersisted() {
        UserEntity owner = user(1L, "alice", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(groupRepository.existsByGroupName("Open")).thenReturn(false);
        when(groupRepository.save(any(GroupEntity.class))).thenAnswer(inv -> {
            GroupEntity group = inv.getArgument(0);
            group.setId(11L);
            return group;
        });
        when(groupMemberRepository.countByGroupId(11L)).thenReturn(1L);

        GroupResponse response = service.create(1L, new GroupCreateRequest("Open", false));

        assertFalse(response.isPrivate());
    }

    @Test
    void createWithTakenGroupNameIsRejected() {
        UserEntity owner = user(1L, "alice", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(groupRepository.existsByGroupName("Team")).thenReturn(true);

        GroupNameTakenException exception = assertThrows(GroupNameTakenException.class,
                () -> service.create(1L, new GroupCreateRequest("Team", null)));

        assertEquals("Group name is already taken", exception.getMessage());
        verify(groupRepository, never()).save(any(GroupEntity.class));
    }

    @Test
    void createWithUnknownUserIsNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(9L, new GroupCreateRequest("Team", null)));

        verify(groupRepository, never()).save(any(GroupEntity.class));
    }

    @Test
    void createNameRaceIsMappedToConflict() {
        UserEntity owner = user(1L, "alice", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(groupRepository.existsByGroupName("Team")).thenReturn(false);
        when(groupRepository.save(any(GroupEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        GroupNameTakenException exception = assertThrows(GroupNameTakenException.class,
                () -> service.create(1L, new GroupCreateRequest("Team", null)));

        assertEquals("Group name is already taken", exception.getMessage());
    }

    @Test
    void memberLeaveRemovesMembership() {
        UserEntity owner = user(1L, "alice", true);
        UserEntity bob = user(2L, "bob", true);
        GroupEntity group = group(1L);
        group.setOwner(owner);
        GroupMemberEntity member = member(group, bob, false);
        when(groupRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 2L)).thenReturn(Optional.of(member));
        when(authorization.isOwner(member, group)).thenReturn(false);

        service.leave(2L, 1L);

        verify(groupMemberRepository).delete(member);
        verify(groupRepository, never()).delete(any(GroupEntity.class));
    }

    @Test
    void ownerCannotLeaveWhileOtherMembersRemain() {
        UserEntity owner = user(1L, "alice", true);
        GroupEntity group = group(1L);
        group.setOwner(owner);
        GroupMemberEntity ownerMember = member(group, owner, true);
        when(groupRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(authorization.isOwner(ownerMember, group)).thenReturn(true);
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(2L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.leave(1L, 1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(groupMemberRepository, never()).delete(any(GroupMemberEntity.class));
        verify(groupRepository, never()).delete(any(GroupEntity.class));
    }

    @Test
    void soleOwnerLeaveDeletesGroup() {
        UserEntity owner = user(1L, "alice", true);
        GroupEntity group = group(1L);
        group.setOwner(owner);
        GroupMemberEntity ownerMember = member(group, owner, true);
        when(groupRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(authorization.isOwner(ownerMember, group)).thenReturn(true);
        when(groupMemberRepository.countByGroupId(1L)).thenReturn(1L);

        service.leave(1L, 1L);

        verify(groupRepository).delete(group);
    }

    @Test
    void nonMemberCannotLeave() {
        GroupEntity group = group(1L);
        when(groupRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.leave(9L, 1L));

        verify(groupMemberRepository, never()).delete(any(GroupMemberEntity.class));
        verify(groupRepository, never()).delete(any(GroupEntity.class));
    }

    @Test
    void ownerTransfersOwnershipToMember() {
        UserEntity owner = user(1L, "alice", true);
        UserEntity bob = user(2L, "bob", true);
        GroupEntity group = group(1L);
        group.setOwner(owner);
        GroupMemberEntity ownerMember = member(group, owner, true);
        GroupMemberEntity targetMember = member(group, bob, false);
        when(groupRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 2L)).thenReturn(Optional.of(targetMember));
        when(authorization.isOwner(ownerMember, group)).thenReturn(true);
        when(groupRepository.save(group)).thenReturn(group);

        service.transferOwnership(1L, 1L, 2L);

        assertEquals(bob, group.getOwner());
        assertTrue(targetMember.isAdmin());
        verify(groupRepository).save(group);
        verify(groupMemberRepository).save(targetMember);
    }

    @Test
    void nonOwnerCannotTransferOwnership() {
        UserEntity owner = user(1L, "alice", true);
        UserEntity bob = user(2L, "bob", true);
        GroupEntity group = group(1L);
        group.setOwner(owner);
        GroupMemberEntity actorMember = member(group, bob, true);
        when(groupRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 2L)).thenReturn(Optional.of(actorMember));
        when(authorization.isOwner(actorMember, group)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.transferOwnership(2L, 1L, 3L));

        verify(groupRepository, never()).save(any(GroupEntity.class));
        verify(groupMemberRepository, never()).save(any(GroupMemberEntity.class));
    }

    @Test
    void ownerCannotTransferToSelf() {
        UserEntity owner = user(1L, "alice", true);
        GroupEntity group = group(1L);
        group.setOwner(owner);
        GroupMemberEntity ownerMember = member(group, owner, true);
        when(groupRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(authorization.isOwner(ownerMember, group)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.transferOwnership(1L, 1L, 1L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(groupRepository, never()).save(any(GroupEntity.class));
    }

    @Test
    void transferTargetMustBeMember() {
        UserEntity owner = user(1L, "alice", true);
        GroupEntity group = group(1L);
        group.setOwner(owner);
        GroupMemberEntity ownerMember = member(group, owner, true);
        when(groupRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 1L)).thenReturn(Optional.of(ownerMember));
        when(groupMemberRepository.findByGroupIdAndUserId(1L, 9L)).thenReturn(Optional.empty());
        when(authorization.isOwner(ownerMember, group)).thenReturn(true);

        assertThrows(ResourceNotFoundException.class,
                () -> service.transferOwnership(1L, 1L, 9L));

        verify(groupRepository, never()).save(any(GroupEntity.class));
    }
}
