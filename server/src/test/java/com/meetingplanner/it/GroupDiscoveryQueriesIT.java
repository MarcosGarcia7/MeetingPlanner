package com.meetingplanner.it;

import com.meetingplanner.common.api.dto.GroupSummaryResponse;
import com.meetingplanner.common.api.model.GroupViewerStatus;
import com.meetingplanner.common.security.InviteTokens;
import com.meetingplanner.domains.group.service.GroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Checks the Groups tab browse queries (suggested / popular) and viewer status against real Postgres. */
class GroupDiscoveryQueriesIT extends AbstractPostgresIT {

    @Autowired
    private GroupService groupService;

    private void addMember(long groupId, long userId) {
        jdbc.update("insert into group_members (group_id, user_id, is_admin) values (?, ?, false)", groupId, userId);
    }

    private void friendship(long userId, long friendId, String status) {
        jdbc.update("insert into friendships (user_id, friend_id, requester_id, status) values (?, ?, ?, ?)",
                userId, friendId, userId, status);
    }

    @Test
    void suggestedRanksByFriendMembersAndSkipsMineBannedAndBlockedOwners() {
        long alice = insertUser("alice");
        long bob = insertUser("bob");
        long carol = insertUser("carol");
        long dave = insertUser("dave");
        long eve = insertUser("eve");
        friendship(alice, bob, "ACCEPTED");
        friendship(carol, alice, "ACCEPTED"); // stored the other way round
        friendship(eve, alice, "BLOCKED");

        long twoFriends = insertGroup("Two friends", dave, true);
        addMember(twoFriends, bob);
        addMember(twoFriends, carol);
        long oneFriend = insertGroup("One friend", bob, false);
        long noFriends = insertGroup("No friends", dave, false);
        long mine = insertGroup("Mine", alice, false);
        addMember(mine, bob);
        long banned = insertGroup("Banned", bob, false);
        jdbc.update("insert into group_bans (group_id, user_id, banned_by_id, banned_by_owner) values (?, ?, ?, true)",
                banned, alice, bob);
        long blockedOwner = insertGroup("Blocked owner", eve, false);
        addMember(blockedOwner, bob);

        List<GroupSummaryResponse> suggested = groupService.listSuggested(alice, 10);

        assertEquals(List.of(twoFriends, oneFriend), suggested.stream().map(GroupSummaryResponse::id).toList());
        assertEquals(2, suggested.getFirst().friendMemberCount());
        assertEquals(3, suggested.getFirst().memberCount());
        assertEquals(GroupViewerStatus.NONE, suggested.getFirst().viewerStatus());

        List<GroupSummaryResponse> popular = groupService.listPopular(alice, 10);
        // Member count desc, then newest first; same exclusions as suggested.
        assertEquals(List.of(twoFriends, noFriends, oneFriend),
                popular.stream().map(GroupSummaryResponse::id).toList());
    }

    @Test
    void summaryReportsPendingRequestAndInvite() {
        long alice = insertUser("alice");
        long bob = insertUser("bob");
        long requested = insertGroup("Requested", bob, true);
        long invited = insertGroup("Invited", bob, true);
        long requestId = jdbc.queryForObject(
                "insert into group_join_requests (requester_id, group_id) values (?, ?) returning id",
                Long.class, alice, requested);
        long inviteId = jdbc.queryForObject("""
                insert into group_invites (sender_id, group_id, recipient_id, recipient_email, token_hash)
                values (?, ?, ?, ?, ?) returning id
                """, Long.class, bob, invited, alice, "alice@example.com",
                InviteTokens.hash(InviteTokens.generateRawToken()));

        List<GroupSummaryResponse> items = groupService.listPopular(alice, 10);

        GroupSummaryResponse requestedItem = items.stream().filter(g -> g.id() == requested).findFirst().orElseThrow();
        assertEquals(GroupViewerStatus.REQUESTED, requestedItem.viewerStatus());
        assertEquals(requestId, requestedItem.pendingRequestId());
        GroupSummaryResponse invitedItem = items.stream().filter(g -> g.id() == invited).findFirst().orElseThrow();
        assertEquals(GroupViewerStatus.INVITED, invitedItem.viewerStatus());
        assertEquals(inviteId, invitedItem.pendingInviteId());
    }
}
