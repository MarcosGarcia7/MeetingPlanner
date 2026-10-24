package com.meetingplanner.domains.friend.service;

import com.meetingplanner.common.api.dto.MemberUsernameRequest;
import com.meetingplanner.domains.friend.model.FriendshipEntity;
import com.meetingplanner.domains.friend.model.FriendshipStatus;
import com.meetingplanner.domains.friend.repository.FriendshipRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BlockService blockService;

    @InjectMocks
    private FriendshipService service;

    private static UserEntity user(long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static FriendshipEntity friendship(UserEntity user, UserEntity friend,
                                               UserEntity requester, FriendshipStatus status) {
        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setUser(user);
        friendship.setFriend(friend);
        friendship.setRequester(requester);
        friendship.setStatus(status);
        return friendship;
    }

    @Test
    void repeatedBlockKeepsOriginalBlockerAndDoesNotInvalidateAgain() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        FriendshipEntity existing = friendship(alice, bob, bob, FriendshipStatus.BLOCKED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(friendshipRepository.insertBlockedIfAbsent(eq(1L), eq(2L), eq(1L), any(OffsetDateTime.class))).thenReturn(0);
        when(friendshipRepository.findByUserIdAndFriendIdForUpdate(1L, 2L)).thenReturn(Optional.of(existing));

        service.block(1L, 2L);

        assertSame(bob, existing.getRequester());
        verify(friendshipRepository, never()).save(any());
        verifyNoInteractions(blockService);
    }

    @Test
    void blockConvertsExistingFriendshipAndInvalidatesPendingInteractions() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        FriendshipEntity existing = friendship(alice, bob, alice, FriendshipStatus.ACCEPTED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(userRepository.getReferenceById(1L)).thenReturn(alice);
        when(friendshipRepository.insertBlockedIfAbsent(eq(1L), eq(2L), eq(1L), any(OffsetDateTime.class))).thenReturn(0);
        when(friendshipRepository.findByUserIdAndFriendIdForUpdate(1L, 2L)).thenReturn(Optional.of(existing));

        service.block(1L, 2L);

        assertEquals(FriendshipStatus.BLOCKED, existing.getStatus());
        assertSame(alice, existing.getRequester());
        verify(friendshipRepository).save(existing);
        verify(blockService).invalidatePendingBetween(1L, 2L);
    }

    @Test
    void onlyBlockerCanUnblock() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        FriendshipEntity existing = friendship(alice, bob, bob, FriendshipStatus.BLOCKED);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendshipRepository.findByUserIdAndFriendIdForUpdate(1L, 2L)).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.unblock(1L, "bob"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(friendshipRepository, never()).delete(any());
    }

    @Test
    void blockerCanUnblockAndRowIsDeleted() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        FriendshipEntity existing = friendship(alice, bob, alice, FriendshipStatus.BLOCKED);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendshipRepository.findByUserIdAndFriendIdForUpdate(1L, 2L)).thenReturn(Optional.of(existing));

        service.unblock(1L, "bob");

        verify(friendshipRepository).delete(existing);
    }

    @Test
    void blockWithoutExistingRowInsertsAtomicallyAndInvalidates() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "alice")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "bob")));
        when(friendshipRepository.insertBlockedIfAbsent(eq(1L), eq(2L), eq(2L), any(OffsetDateTime.class))).thenReturn(1);

        service.block(2L, 1L);

        verify(friendshipRepository, never()).findByUserIdAndFriendIdForUpdate(any(), any());
        verify(friendshipRepository, never()).save(any());
        verify(blockService).invalidatePendingBetween(2L, 1L);
    }

    @Test
    void blockWhenRowVanishesAfterInsertConflictIsRetryableConflict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "alice")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "bob")));
        when(friendshipRepository.insertBlockedIfAbsent(eq(1L), eq(2L), eq(1L), any(OffsetDateTime.class))).thenReturn(0);
        when(friendshipRepository.findByUserIdAndFriendIdForUpdate(1L, 2L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.block(1L, 2L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(blockService);
    }

    @Test
    void concurrentFirstFriendRequestsMapInsertRaceToConflict() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendshipRepository.findByUserIdAndFriendIdForUpdate(1L, 2L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(alice);
        when(userRepository.getReferenceById(2L)).thenReturn(bob);
        when(friendshipRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("friendships_pkey"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.sendRequest(1L, new MemberUsernameRequest("bob")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    void mutualRequestAcceptsLockedPendingRow() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        FriendshipEntity pending = friendship(alice, bob, bob, FriendshipStatus.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendshipRepository.findByUserIdAndFriendIdForUpdate(1L, 2L)).thenReturn(Optional.of(pending));

        service.sendRequest(1L, new MemberUsernameRequest("bob"));

        assertEquals(FriendshipStatus.ACCEPTED, pending.getStatus());
        verify(friendshipRepository).save(pending);
        verify(friendshipRepository, never()).findByUserIdAndFriendId(any(), any());
    }

    @Test
    void respondToAlreadyResolvedRequestIsConflict() {
        UserEntity alice = user(1L, "alice");
        UserEntity bob = user(2L, "bob");
        FriendshipEntity accepted = friendship(alice, bob, bob, FriendshipStatus.ACCEPTED);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendshipRepository.findByUserIdAndFriendIdForUpdate(1L, 2L)).thenReturn(Optional.of(accepted));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.respond(1L, "bob", false));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(FriendshipStatus.ACCEPTED, accepted.getStatus());
    }
}
