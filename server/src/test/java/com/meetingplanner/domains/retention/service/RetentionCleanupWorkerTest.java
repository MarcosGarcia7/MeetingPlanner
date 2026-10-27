package com.meetingplanner.domains.retention.service;

import com.meetingplanner.domains.email.repository.EmailOutboxRepository;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import com.meetingplanner.domains.group.repository.GroupInviteRepository;
import com.meetingplanner.domains.group.repository.GroupJoinRequestRepository;
import com.meetingplanner.domains.notification.repository.NotificationRepository;
import com.meetingplanner.domains.user.repository.AccountTokenRepository;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetentionCleanupWorkerTest {

    @Mock
    private EmailOutboxRepository emailOutboxRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private GroupJoinRequestRepository groupJoinRequestRepository;
    @Mock
    private GroupInviteRepository groupInviteRepository;
    @Mock
    private AccountTokenRepository accountTokenRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RetentionCleanupWorker worker;

    @Test
    void cleanupPassesRetentionCutoffs() {
        OffsetDateTime before = OffsetDateTime.now();
        worker.cleanup();

        ArgumentCaptor<OffsetDateTime> sent = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> failed = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(emailOutboxRepository).deleteSentAndFailed(sent.capture(), failed.capture());
        assertAgo(before, sent.getValue(), Duration.ofDays(7));
        assertAgo(before, failed.getValue(), Duration.ofDays(30));

        ArgumentCaptor<OffsetDateTime> read = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> created = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(notificationRepository).deleteOld(read.capture(), created.capture());
        assertAgo(before, read.getValue(), Duration.ofDays(30));
        assertAgo(before, created.getValue(), Duration.ofDays(90));

        ArgumentCaptor<OffsetDateTime> requests = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(groupJoinRequestRepository).deleteRespondedBefore(requests.capture());
        assertAgo(before, requests.getValue(), Duration.ofDays(30));

        ArgumentCaptor<OffsetDateTime> expired = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> responded = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(groupInviteRepository).deleteExpiredAndResponded(expired.capture(), responded.capture());
        assertAgo(before, expired.getValue(), GroupInviteEntity.EXPIRY);
        assertAgo(before, responded.getValue(), Duration.ofDays(30));

        ArgumentCaptor<OffsetDateTime> tokens = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(accountTokenRepository).deleteExpiredOrConsumed(tokens.capture());
        assertAgo(before, tokens.getValue(), Duration.ZERO);

        ArgumentCaptor<OffsetDateTime> users = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(userRepository).deleteUnverifiedCreatedBefore(users.capture());
        assertAgo(before, users.getValue(), Duration.ofDays(7));
    }

    @Test
    void failingPurgeDoesNotStopTheOthers() {
        when(emailOutboxRepository.deleteSentAndFailed(any(), any())).thenThrow(new IllegalStateException("db down"));
        when(groupInviteRepository.deleteExpiredAndResponded(any(), any())).thenThrow(new IllegalStateException("db down"));

        worker.cleanup();

        verify(notificationRepository).deleteOld(any(), any());
        verify(groupJoinRequestRepository).deleteRespondedBefore(any());
        verify(accountTokenRepository).deleteExpiredOrConsumed(any());
        verify(userRepository).deleteUnverifiedCreatedBefore(any());
    }

    // The worker reads the clock once, just after `before`, so each cutoff lands within a second of before - age.
    private static void assertAgo(OffsetDateTime before, OffsetDateTime cutoff, Duration age) {
        OffsetDateTime expected = before.minus(age);
        assertTrue(!cutoff.isBefore(expected) && cutoff.isBefore(expected.plusSeconds(1)),
                () -> "expected ~" + expected + " but was " + cutoff);
    }
}
