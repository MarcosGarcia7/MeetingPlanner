package com.meetingplanner.domains.notification.service;

import com.meetingplanner.domains.email.repository.EmailOutboxRepository;
import com.meetingplanner.domains.notification.model.NotificationEntity;
import com.meetingplanner.domains.notification.model.NotificationPreferenceEntity;
import com.meetingplanner.domains.notification.repository.NotificationPreferenceRepository;
import com.meetingplanner.domains.notification.repository.NotificationRepository;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private EmailOutboxRepository emailOutboxRepository;
    @Mock
    private UserRepository userRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(preferenceRepository, notificationRepository, emailOutboxRepository,
                userRepository, new ObjectMapper());
    }

    private static NotificationPreferenceEntity prefs(boolean meetingInvites) {
        NotificationPreferenceEntity prefs = new NotificationPreferenceEntity();
        prefs.setEmailEnabled(true);
        prefs.setMeetingInvites(meetingInvites);
        return prefs;
    }

    @Test
    void pushInAppSavesNotificationWithoutQueueingEmail() {
        UserEntity bob = new UserEntity();
        bob.setId(2L);
        when(preferenceRepository.findOrCreate(2L)).thenReturn(prefs(true));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));

        service.pushInApp(2L, "MEETING_INVITE", "meeting_invites", Map.of("sender", "alice", "title", "Standup"));

        ArgumentCaptor<NotificationEntity> saved = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(saved.capture());
        assertEquals("MEETING_INVITE", saved.getValue().getType());
        assertTrue(saved.getValue().getPayload().contains("\"sender\":\"alice\""));
        verifyNoInteractions(emailOutboxRepository);
    }

    @Test
    void pushInAppRespectsMutedCategory() {
        when(preferenceRepository.findOrCreate(2L)).thenReturn(prefs(false));

        service.pushInApp(2L, "MEETING_INVITE", "meeting_invites", Map.of("sender", "alice"));

        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(userRepository, emailOutboxRepository);
    }
}
