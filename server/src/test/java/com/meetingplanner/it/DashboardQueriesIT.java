package com.meetingplanner.it;

import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.PostResponse;
import com.meetingplanner.common.api.dto.TimeChangeProposalResponse;
import com.meetingplanner.common.api.dto.UserSearchResult;
import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.common.api.model.UserRelationship;
import com.meetingplanner.domains.friend.service.FriendshipService;
import com.meetingplanner.domains.group.service.GroupService;
import com.meetingplanner.domains.group.service.PostService;
import com.meetingplanner.domains.invite.service.InviteService;
import com.meetingplanner.domains.meeting.service.MeetingService;
import com.meetingplanner.domains.meeting.service.MeetingTimeChangeService;
import com.meetingplanner.domains.notification.service.NotificationFeedService;
import com.meetingplanner.domains.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Checks the cross-group JPQL behind the dashboard against real Postgres. */
class DashboardQueriesIT extends AbstractPostgresIT {

    @Autowired
    private PostService postService;
    @Autowired
    private MeetingTimeChangeService timeChangeService;
    @Autowired
    private MeetingService meetingService;
    @Autowired
    private InviteService inviteService;
    @Autowired
    private FriendshipService friendshipService;
    @Autowired
    private NotificationFeedService notificationFeedService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private UserService userService;

    private long insertPost(long groupId, long authorId, String body, OffsetDateTime createdAt) {
        return jdbc.queryForObject(
                "insert into group_posts (group_id, author_id, body, created_at) values (?, ?, ?, ?) returning id",
                Long.class, groupId, authorId, body, Timestamp.from(createdAt.toInstant()));
    }

    @Test
    void recentPostsCoverMyGroupsOnlyAndSkipBlockedAuthors() {
        long alice = insertUser("alice");
        long bob = insertUser("bob");
        long carol = insertUser("carol");
        long books = insertGroup("Book club", alice, false);
        long hiking = insertGroup("Hiking", bob, false);
        long secret = insertGroup("Not mine", carol, false);
        jdbc.update("insert into group_members (group_id, user_id, is_admin) values (?, ?, false)", hiking, alice);
        jdbc.update("insert into group_members (group_id, user_id, is_admin) values (?, ?, false)", books, carol);
        // carol blocked alice: carol's posts disappear from alice's feed.
        jdbc.update("insert into friendships (user_id, friend_id, requester_id, status) values (?, ?, ?, 'BLOCKED')",
                carol, alice, carol);

        OffsetDateTime now = OffsetDateTime.now();
        insertPost(books, alice, "mine", now.minusHours(3));
        insertPost(hiking, bob, "bob's", now.minusHours(2));
        insertPost(books, carol, "blocked", now.minusHours(1));
        insertPost(secret, carol, "other group", now);

        CursorPage<PostResponse> first = postService.listRecent(alice, null, 1);
        assertEquals(List.of("bob's"), first.items().stream().map(PostResponse::body).toList());
        assertEquals("Hiking", first.items().getFirst().groupName());

        CursorPage<PostResponse> second = postService.listRecent(alice, first.nextCursor(), 5);
        assertEquals(List.of("mine"), second.items().stream().map(PostResponse::body).toList());
        assertEquals(null, second.nextCursor());
    }

    // A null cursor binds untyped nulls; Postgres rejects `? is null` unless the parameter is typed.
    @Test
    void firstPagesLoadWithoutCursor() {
        long alice = insertUser("alice");
        long books = insertGroup("Book club", alice, false);
        insertPost(books, alice, "mine", OffsetDateTime.now());

        assertDoesNotThrow(() -> postService.list(alice, books, null, 5));
        assertDoesNotThrow(() -> meetingService.listMine(alice, null, null, 5));
        assertDoesNotThrow(() -> inviteService.listMine(alice, null, null, 5));
        assertDoesNotThrow(() -> inviteService.listMine(alice, InviteStatus.PENDING, null, 5));
        assertDoesNotThrow(() -> friendshipService.listIncomingRequests(alice, null, 5));
        assertDoesNotThrow(() -> friendshipService.listFriends(alice, null, 5));
        assertDoesNotThrow(() -> notificationFeedService.list(alice, null, 5));
        assertDoesNotThrow(() -> groupService.listMine(alice, null, 5));
        assertDoesNotThrow(() -> groupService.search(alice, "book", null, 5));
        assertDoesNotThrow(() -> userService.search(alice, "ali", null, 5));
    }

    @Test
    void userSearchHidesSelfUnverifiedAndBlockedPairsAndReportsRelationship() {
        long alice = insertUser("sam_alice");
        long friend = insertUser("sam_friend");
        long pending = insertUser("sam_pending");
        long blockedByAlice = insertUser("sam_blocked");
        long blockedAlice = insertUser("sam_blocker");
        long unverified = insertUser("sam_unverified");
        jdbc.update("update users set email_verified_at = null where id = ?", unverified);
        insertFriendship(alice, friend, alice, "ACCEPTED");
        insertFriendship(alice, pending, pending, "PENDING");
        insertFriendship(alice, blockedByAlice, alice, "BLOCKED");
        insertFriendship(alice, blockedAlice, blockedAlice, "BLOCKED");

        CursorPage<UserSearchResult> page = userService.search(alice, "sam", null, 20);

        assertEquals(List.of("sam_friend", "sam_pending"),
                page.items().stream().map(UserSearchResult::username).toList());
        assertEquals(List.of(UserRelationship.FRIENDS, UserRelationship.REQUEST_RECEIVED),
                page.items().stream().map(UserSearchResult::relationship).toList());
    }

    private void insertFriendship(long a, long b, long requester, String status) {
        jdbc.update("insert into friendships (user_id, friend_id, requester_id, status) values (?, ?, ?, ?)",
                Math.min(a, b), Math.max(a, b), requester, status);
    }

    @Test
    void awaitingVoteListsOnlyMyPendingVotesOnPendingProposals() {
        long alice = insertUser("alice");
        long bob = insertUser("bob");
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        long open = insertMeeting(null, alice, start, bob);
        long voted = insertMeeting(null, alice, start.plusDays(1), bob);

        long openProposal = jdbc.queryForObject("""
                insert into meeting_time_change_proposals (meeting_id, proposed_by, proposed_start_time)
                values (?, ?, ?) returning id
                """, Long.class, open, alice, Timestamp.from(start.plusHours(1).toInstant()));
        jdbc.update("insert into meeting_time_change_votes (proposal_id, user_id) values (?, ?)", openProposal, bob);
        long votedProposal = jdbc.queryForObject("""
                insert into meeting_time_change_proposals (meeting_id, proposed_by, proposed_start_time)
                values (?, ?, ?) returning id
                """, Long.class, voted, alice, Timestamp.from(start.plusDays(1).plusHours(1).toInstant()));
        jdbc.update("insert into meeting_time_change_votes (proposal_id, user_id, status) values (?, ?, 'APPROVED')",
                votedProposal, bob);

        List<TimeChangeProposalResponse> bobs = timeChangeService.listAwaitingVote(bob);
        assertEquals(List.of(openProposal), bobs.stream().map(TimeChangeProposalResponse::id).toList());
        assertEquals("IT meeting", bobs.getFirst().meetingTitle());
        assertEquals(List.of(), timeChangeService.listAwaitingVote(alice));
    }
}
