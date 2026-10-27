package com.meetingplanner.it;

import com.meetingplanner.domains.retention.service.RetentionCleanupWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Runs the purge queries against real Postgres to check the JPQL and the FK cascades they rely on.
class RetentionCleanupIT extends AbstractPostgresIT {

    @Autowired
    private RetentionCleanupWorker worker;

    @Test
    void purgesStaleRowsAndKeepsLiveOnes() {
        long alice = insertUser("alice");
        long bob = insertUser("bob");
        long groupId = insertGroup("Book club", alice, true);

        // email_outbox: old SENT and old FAILED go; recent SENT, old-but-PENDING stay.
        insertEmail(alice, "SENT", "now() - interval '8 days'", "now() - interval '8 days'");
        insertEmail(alice, "SENT", "now() - interval '1 day'", "now() - interval '1 day'");
        insertEmail(alice, "FAILED", "now() - interval '31 days'", null);
        insertEmail(alice, "PENDING", "now() - interval '60 days'", null);

        // notifications: read past 30d goes, unread past 90d goes, unread at 60d stays.
        insertNotification(bob, "now() - interval '31 days'", "now() - interval '31 days'");
        insertNotification(bob, null, "now() - interval '91 days'");
        insertNotification(bob, null, "now() - interval '60 days'");

        // group_join_requests: answered past 30d goes, pending stays regardless of age.
        jdbc.update("""
                insert into group_join_requests (requester_id, group_id, status, responded_at, time_sent)
                values (?, ?, 'CANCELLED', now() - interval '31 days', now() - interval '32 days'),
                       (?, ?, 'PENDING', null, now() - interval '60 days')
                """, bob, groupId, bob, groupId);

        // group_invites: pending past expiry goes, answered past 30d goes, fresh pending stays.
        long carol = insertUser("carol");
        long dave = insertUser("dave");
        insertGroupInvite(groupId, alice, bob, "PENDING", "now() - interval '8 days'", null);
        insertGroupInvite(groupId, alice, carol, "DECLINED", "now() - interval '40 days'", "now() - interval '31 days'");
        insertGroupInvite(groupId, alice, dave, "PENDING", "now() - interval '6 days'", null);

        // account_tokens: expired and consumed go, live one stays.
        insertToken(alice, "a", "now() - interval '1 hour'", null);
        insertToken(alice, "b", "now() + interval '1 hour'", "now()");
        insertToken(bob, "c", "now() + interval '1 hour'", null);

        // users: unverified past 7d goes (with its rows); a fresh unverified one stays.
        long stale = insertUnverifiedUser("stale", "now() - interval '8 days'");
        insertUnverifiedUser("fresh", "now() - interval '1 day'");
        insertToken(stale, "d", "now() + interval '1 hour'", null);

        worker.cleanup();

        assertEquals(2, count("select count(*) from email_outbox"));
        assertEquals(1, count("select count(*) from email_outbox where status = 'PENDING'"));
        assertEquals(1, count("select count(*) from notifications"));
        assertEquals(1, count("select count(*) from group_join_requests where status = 'PENDING'"));
        assertEquals(1, count("select count(*) from group_join_requests"));
        assertEquals(1, count("select count(*) from group_invites"));
        assertEquals(1, count("select count(*) from group_invites where recipient_id = ?", dave));
        assertEquals(1, count("select count(*) from account_tokens"));
        assertEquals(1, count("select count(*) from account_tokens where user_id = ?", bob));
        assertEquals(0, count("select count(*) from users where id = ?", stale));
        assertEquals(1, count("select count(*) from users where username = 'fresh'"));
        assertEquals(5, count("select count(*) from users"));
    }

    private void insertEmail(long userId, String status, String createdAt, String sentAt) {
        jdbc.update("insert into email_outbox (recipient_user_id, recipient_email, template, status, created_at, sent_at) "
                + "values (?, 'x@example.com', 'T', ?, " + createdAt + ", " + (sentAt == null ? "null" : sentAt) + ")",
                userId, status);
    }

    private void insertNotification(long userId, String readAt, String createdAt) {
        jdbc.update("insert into notifications (user_id, type, read_at, created_at) values (?, 'X', "
                + (readAt == null ? "null" : readAt) + ", " + createdAt + ")", userId);
    }

    private void insertGroupInvite(long groupId, long senderId, long recipientId, String status,
                                   String timeSent, String respondedAt) {
        jdbc.update("insert into group_invites (sender_id, group_id, recipient_id, recipient_email, token_hash, "
                + "status, time_sent, responded_at) values (?, ?, ?, ?, repeat('0', 64), ?, " + timeSent + ", "
                + (respondedAt == null ? "null" : respondedAt) + ")",
                senderId, groupId, recipientId, "user" + recipientId + "@example.com", status);
    }

    private void insertToken(long userId, String hashChar, String expiresAt, String consumedAt) {
        jdbc.update("insert into account_tokens (user_id, token_hash, token_type, expires_at, consumed_at) "
                + "values (?, repeat(?, 64), 'EMAIL_VERIFY', " + expiresAt + ", "
                + (consumedAt == null ? "null" : consumedAt) + ")", userId, hashChar);
    }

    private long insertUnverifiedUser(String username, String createdAt) {
        return jdbc.queryForObject("insert into users (username, email, password_hash, avatar_color, created_at) "
                + "values (?, ?, 'x', '#000000', " + createdAt + ") returning id",
                Long.class, username, username + "@example.com");
    }
}
