package com.meetingplanner.domains.retention.service;

import com.meetingplanner.domains.email.repository.EmailOutboxRepository;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import com.meetingplanner.domains.group.repository.GroupInviteRepository;
import com.meetingplanner.domains.group.repository.GroupJoinRequestRepository;
import com.meetingplanner.domains.notification.repository.NotificationRepository;
import com.meetingplanner.domains.user.repository.AccountTokenRepository;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.function.IntSupplier;

// Purges rows that aren't tied to a meeting (meeting-scoped rows go with MeetingCleanupWorker's cascade).
// Each purge runs in its own transaction so one failure doesn't block the rest.
@Component
public class RetentionCleanupWorker {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupWorker.class);

    private static final int SENT_EMAIL_RETENTION_DAYS = 7;
    private static final int FAILED_EMAIL_RETENTION_DAYS = 30;
    private static final int READ_NOTIFICATION_RETENTION_DAYS = 30;
    private static final int NOTIFICATION_RETENTION_DAYS = 90;
    private static final int RESPONDED_REQUEST_RETENTION_DAYS = 30;
    private static final int RESPONDED_INVITE_RETENTION_DAYS = 30;
    private static final int UNVERIFIED_USER_RETENTION_DAYS = 7;

    private final EmailOutboxRepository emailOutboxRepository;
    private final NotificationRepository notificationRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final AccountTokenRepository accountTokenRepository;
    private final UserRepository userRepository;

    public RetentionCleanupWorker(EmailOutboxRepository emailOutboxRepository,
                                  NotificationRepository notificationRepository,
                                  GroupJoinRequestRepository groupJoinRequestRepository,
                                  GroupInviteRepository groupInviteRepository,
                                  AccountTokenRepository accountTokenRepository,
                                  UserRepository userRepository) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.notificationRepository = notificationRepository;
        this.groupJoinRequestRepository = groupJoinRequestRepository;
        this.groupInviteRepository = groupInviteRepository;
        this.accountTokenRepository = accountTokenRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "${app.retention.cleanup-cron:0 30 3 * * *}", zone = "UTC")
    public void cleanup() {
        OffsetDateTime now = OffsetDateTime.now();

        purge("email_outbox", () -> emailOutboxRepository.deleteSentAndFailed(
                now.minusDays(SENT_EMAIL_RETENTION_DAYS), now.minusDays(FAILED_EMAIL_RETENTION_DAYS)));
        purge("notifications", () -> notificationRepository.deleteOld(
                now.minusDays(READ_NOTIFICATION_RETENTION_DAYS), now.minusDays(NOTIFICATION_RETENTION_DAYS)));
        purge("group_join_requests", () -> groupJoinRequestRepository.deleteRespondedBefore(
                now.minusDays(RESPONDED_REQUEST_RETENTION_DAYS)));
        purge("group_invites", () -> groupInviteRepository.deleteExpiredAndResponded(
                now.minus(GroupInviteEntity.EXPIRY), now.minusDays(RESPONDED_INVITE_RETENTION_DAYS)));
        purge("account_tokens", () -> accountTokenRepository.deleteExpiredOrConsumed(now));
        purge("users", () -> userRepository.deleteUnverifiedCreatedBefore(
                now.minusDays(UNVERIFIED_USER_RETENTION_DAYS)));
    }

    private void purge(String table, IntSupplier delete) {
        try {
            int deleted = delete.getAsInt();
            if (deleted > 0) {
                log.info("event=retention_purge table={} deleted={}", table, deleted);
            }
        } catch (RuntimeException e) {
            log.error("event=retention_purge_failed table={} error={}", table, e.getMessage(), e);
        }
    }
}
