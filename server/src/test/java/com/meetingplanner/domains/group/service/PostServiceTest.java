package com.meetingplanner.domains.group.service;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.PostResponse;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupPostEntity;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupPostCommentRepository;
import com.meetingplanner.domains.group.repository.GroupPostRepository;
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
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private GroupPostRepository postRepository;
    @Mock
    private GroupPostCommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PostService service;

    private static UserEntity user(long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static GroupEntity group(long id, String name) {
        GroupEntity group = new GroupEntity();
        group.setId(id);
        group.setGroupName(name);
        return group;
    }

    private static GroupPostEntity post(long id, GroupEntity group, UserEntity author, OffsetDateTime createdAt) {
        GroupPostEntity post = new GroupPostEntity();
        post.setId(id);
        post.setGroup(group);
        post.setAuthor(author);
        post.setBody("Hello " + id);
        post.setCreatedAt(createdAt);
        return post;
    }

    @Test
    void listRecentMapsGroupNamesAndHasNoCursorOnLastPage() {
        GroupEntity books = group(3L, "Book club");
        GroupEntity hiking = group(4L, "Hiking");
        UserEntity bob = user(2L, "bob");
        OffsetDateTime now = OffsetDateTime.parse("2026-10-25T12:00:00Z");
        when(postRepository.listRecentForMember(eq(1L), isNull(), isNull(), any()))
                .thenReturn(List.of(post(11L, books, bob, now), post(10L, hiking, bob, now.minusHours(1))));

        CursorPage<PostResponse> page = service.listRecent(1L, null, 5);

        assertEquals(2, page.items().size());
        assertEquals("Book club", page.items().get(0).groupName());
        assertEquals("Hiking", page.items().get(1).groupName());
        assertEquals("bob", page.items().get(0).authorUsername());
        assertNull(page.nextCursor());
        verifyNoInteractions(groupMemberRepository);
    }

    @Test
    void listRecentFetchesOneExtraRowToDetectNextPage() {
        GroupEntity books = group(3L, "Book club");
        UserEntity bob = user(2L, "bob");
        OffsetDateTime now = OffsetDateTime.parse("2026-10-25T12:00:00Z");
        when(postRepository.listRecentForMember(eq(1L), isNull(), isNull(), any()))
                .thenReturn(List.of(
                        post(12L, books, bob, now),
                        post(11L, books, bob, now.minusMinutes(1)),
                        post(10L, books, bob, now.minusMinutes(2))));

        CursorPage<PostResponse> page = service.listRecent(1L, null, 2);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).listRecentForMember(eq(1L), isNull(), isNull(), pageable.capture());
        assertEquals(3, pageable.getValue().getPageSize());
        assertEquals(List.of(12L, 11L), page.items().stream().map(PostResponse::id).toList());
        assertNotNull(page.nextCursor());
    }

    @Test
    void listRecentDecodesCursorForNextPage() {
        GroupEntity books = group(3L, "Book club");
        UserEntity bob = user(2L, "bob");
        OffsetDateTime now = OffsetDateTime.parse("2026-10-25T12:00:00Z");
        when(postRepository.listRecentForMember(eq(1L), isNull(), isNull(), any()))
                .thenReturn(List.of(post(12L, books, bob, now), post(11L, books, bob, now.minusMinutes(1))));
        String cursor = service.listRecent(1L, null, 1).nextCursor();
        when(postRepository.listRecentForMember(eq(1L), eq(now), eq(12L), any())).thenReturn(List.of());

        CursorPage<PostResponse> next = service.listRecent(1L, cursor, 1);

        assertEquals(List.of(), next.items());
        assertNull(next.nextCursor());
    }
}
