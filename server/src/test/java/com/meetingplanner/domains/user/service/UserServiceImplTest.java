package com.meetingplanner.domains.user.service;

import com.meetingplanner.common.api.SearchCursorCodec;
import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.UserProfileResponse;
import com.meetingplanner.common.api.dto.UserProfileUpdateRequest;
import com.meetingplanner.common.api.dto.UserSearchResult;
import com.meetingplanner.common.api.model.UserRelationship;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private AccountTokenService accountTokenService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private JtiStorageService jtiStorageService;

    @InjectMocks
    private UserServiceImpl service;

    private static UserEntity user(long id) {
        UserEntity user = new UserEntity("alice", "alice@example.com", "hash", "#3B82F6", false);
        user.setId(id);
        return user;
    }

    private static UserEntity user(long id, String username) {
        UserEntity user = new UserEntity(username, username + "@example.com", "hash", "#3B82F6", false);
        user.setId(id);
        return user;
    }

    private static FriendshipEntity friendship(UserEntity a, UserEntity b, UserEntity requester,
                                               FriendshipStatus status) {
        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setUser(a.getId() < b.getId() ? a : b);
        friendship.setFriend(a.getId() < b.getId() ? b : a);
        friendship.setRequester(requester);
        friendship.setStatus(status);
        return friendship;
    }

    private static UserProfileUpdateRequest onlyTimezone(String timezone) {
        return new UserProfileUpdateRequest(null, null, null, timezone, null, null, null);
    }

    @Test
    void searchMapsMinimalUserFields() {
        UserEntity alice = user(1L);
        alice.setFirstName("Alice");
        alice.setLastName("Smith");
        when(userRepository.search(99L, "%alice%", null, null, PageRequest.of(0, 21)))
                .thenReturn(List.of(alice));

        CursorPage<UserSearchResult> page = service.search(99L, " ALICE ", null, 20);

        assertEquals(1, page.items().size());
        UserSearchResult result = page.items().getFirst();
        assertEquals(1L, result.id());
        assertEquals("alice", result.username());
        assertEquals("Alice", result.firstName());
        assertEquals("Smith", result.lastName());
        assertEquals("#3B82F6", result.avatarColor());
        assertEquals(UserRelationship.NONE, result.relationship());
        assertNull(page.nextCursor());
    }

    @Test
    void searchReportsRelationshipFromViewersSide() {
        UserEntity viewer = user(5L, "viewer");
        UserEntity friend = user(1L, "friend");
        UserEntity sentTo = user(2L, "sentto");
        UserEntity receivedFrom = user(7L, "receivedfrom");
        UserEntity declined = user(8L, "declined");
        UserEntity stranger = user(9L, "stranger");
        when(userRepository.search(5L, "%e%", null, null, PageRequest.of(0, 21)))
                .thenReturn(List.of(friend, sentTo, receivedFrom, declined, stranger));
        when(friendshipRepository.findBetweenUserAnd(5L, List.of(1L, 2L, 7L, 8L, 9L)))
                .thenReturn(List.of(
                        friendship(viewer, friend, friend, FriendshipStatus.ACCEPTED),
                        friendship(viewer, sentTo, viewer, FriendshipStatus.PENDING),
                        friendship(viewer, receivedFrom, receivedFrom, FriendshipStatus.PENDING),
                        friendship(viewer, declined, viewer, FriendshipStatus.DECLINED)));

        CursorPage<UserSearchResult> page = service.search(5L, "e", null, 20);

        assertEquals(List.of(
                        UserRelationship.FRIENDS,
                        UserRelationship.REQUEST_SENT,
                        UserRelationship.REQUEST_RECEIVED,
                        UserRelationship.NONE,
                        UserRelationship.NONE),
                page.items().stream().map(UserSearchResult::relationship).toList());
    }

    @Test
    void searchSkipsRelationshipLookupWhenNothingMatches() {
        when(userRepository.search(5L, "%zzz%", null, null, PageRequest.of(0, 21)))
                .thenReturn(List.of());

        CursorPage<UserSearchResult> page = service.search(5L, "zzz", null, 20);

        assertEquals(0, page.items().size());
        verify(friendshipRepository, never()).findBetweenUserAnd(any(), any());
    }

    @Test
    void searchPagesWithCaseInsensitiveUsernameCursor() {
        UserEntity alice = user(1L);
        UserEntity bob = user(2L);
        when(userRepository.search(99L, "%a%", null, null, PageRequest.of(0, 2)))
                .thenReturn(List.of(alice, bob));

        CursorPage<UserSearchResult> page = service.search(99L, "a", null, 1);

        assertEquals(1, page.items().size());
        assertEquals("alice", page.items().getFirst().username());
        assertNotNull(page.nextCursor());
        SearchCursorCodec.Cursor decoded = SearchCursorCodec.decode(page.nextCursor());
        assertEquals("alice", decoded.value());
        assertEquals(1L, decoded.id());
    }

    @Test
    void updateMeSetsTimezone() {
        UserEntity user = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = service.updateMe(1L, onlyTimezone("America/New_York"));

        assertEquals("America/New_York", user.getTimezone());
        assertEquals("America/New_York", response.timezone());
    }

    @Test
    void updateMeClearsTimezoneOnBlank() {
        UserEntity user = user(1L);
        user.setTimezone("America/New_York");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = service.updateMe(1L, onlyTimezone("  "));

        assertNull(user.getTimezone());
        assertNull(response.timezone());
    }

    @Test
    void updateMeLeavesTimezoneUntouchedWhenNull() {
        UserEntity user = user(1L);
        user.setTimezone("Europe/Paris");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.updateMe(1L, onlyTimezone(null));

        assertEquals("Europe/Paris", user.getTimezone());
    }

    @Test
    void updateMeRejectsInvalidTimezone() {
        UserEntity user = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateMe(1L, onlyTimezone("Not/AZone")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void getMeIncludesPendingEmail() {
        UserEntity user = user(1L);
        user.setPendingEmail("new@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = service.getMe(1L);

        assertEquals("alice@example.com", response.email());
        assertEquals("new@example.com", response.pendingEmail());
    }

    @Test
    void getByUsernameHidesEmailsFromOtherViewers() {
        UserEntity user = user(1L);
        user.setPendingEmail("new@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserProfileResponse response = service.getByUsername(2L, "alice");

        assertNull(response.email());
        assertNull(response.pendingEmail());
    }
}
