package com.meetingplanner.it;

import com.meetingplanner.common.security.InviteTokens;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Base for opt-in PostgreSQL integration tests (run with {@code mvn verify -Pit}; needs Docker).
 * One container is shared by every IT class so the cached Spring context keeps pointing at it.
 */
@SpringBootTest(properties = {
        "app.scheduling.enabled=false",
        "app.email.enabled=false",
        "JWT_ISSUER=meetingplanner-it",
        "JWT_ACCESS_SECRET=it-access-secret-it-access-secret-it-access-secret-0123456789",
        "JWT_REFRESH_SECRET=it-refresh-secret-it-refresh-secret-it-refresh-secret-0123456",
        "ACCESS_TOKEN_EXPIRATION_MS=900000",
        "REFRESH_TOKEN_EXPIRATION_MS=86400000"
})
abstract class AbstractPostgresIT {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @AfterEach
    void truncate() {
        // Every domain table references users, so this cascades to all of them.
        jdbc.execute("TRUNCATE users RESTART IDENTITY CASCADE");
    }

    protected long insertUser(String username) {
        return jdbc.queryForObject("""
                insert into users (username, email, password_hash, avatar_color, email_verified_at)
                values (?, ?, 'x', '#000000', now())
                returning id
                """, Long.class, username, username + "@example.com");
    }

    protected long insertGroup(String name, long ownerId, boolean isPrivate) {
        long groupId = jdbc.queryForObject(
                "insert into groups (group_name, owner_id, is_private) values (?, ?, ?) returning id",
                Long.class, name, ownerId, isPrivate);
        jdbc.update("insert into group_members (group_id, user_id, is_admin) values (?, ?, true)", groupId, ownerId);
        return groupId;
    }

    protected long insertMeeting(Long groupId, long creatorId, OffsetDateTime start, long... participantIds) {
        long meetingId = jdbc.queryForObject("""
                insert into meetings (group_id, start_time, title, creator_id)
                values (?, ?, 'IT meeting', ?)
                returning id
                """, Long.class, groupId, Timestamp.from(start.toInstant()), creatorId);
        jdbc.update("insert into meeting_participants (meeting_id, user_id) values (?, ?)", meetingId, creatorId);
        for (long participantId : participantIds) {
            jdbc.update("insert into meeting_participants (meeting_id, user_id) values (?, ?)", meetingId, participantId);
        }
        return meetingId;
    }

    protected long insertPendingMeetingInvite(long meetingId, long senderId, long recipientId, String recipientEmail) {
        return jdbc.queryForObject("""
                insert into meeting_invites (sender_id, meeting_id, recipient_id, recipient_email, token_hash)
                values (?, ?, ?, ?, ?)
                returning id
                """, Long.class, senderId, meetingId, recipientId, recipientEmail,
                InviteTokens.hash(InviteTokens.generateRawToken()));
    }

    protected long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    /** Releases every task at once and returns each task's outcome (value or thrown exception). */
    @SafeVarargs
    protected static <T> List<Outcome<T>> runConcurrently(Callable<T>... tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.length);
        try {
            CountDownLatch ready = new CountDownLatch(tasks.length);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            ready.await();
            start.countDown();

            List<Outcome<T>> outcomes = new ArrayList<>();
            for (Future<T> future : futures) {
                try {
                    outcomes.add(new Outcome<>(future.get(30, TimeUnit.SECONDS), null));
                } catch (java.util.concurrent.ExecutionException e) {
                    outcomes.add(new Outcome<>(null, e.getCause()));
                }
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    protected record Outcome<T>(T value, Throwable error) {
        boolean succeeded() {
            return error == null;
        }
    }
}
