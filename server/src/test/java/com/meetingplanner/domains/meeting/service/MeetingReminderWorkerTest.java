package com.meetingplanner.domains.meeting.service;

import com.meetingplanner.domains.meeting.model.MeetingEntity;
import com.meetingplanner.domains.meeting.repository.MeetingReminderSentRepository;
import com.meetingplanner.domains.meeting.repository.MeetingRepository;
import com.meetingplanner.domains.notification.repository.NotificationPreferenceRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingReminderWorkerTest {

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingReminderSentRepository reminderSentRepository;
    @Mock
    private NotificationPreferenceRepository preferenceRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MeetingReminderWorker worker;

    private static MeetingEntity meetingStartingIn(long minutes, UserEntity creator) {
        MeetingEntity meeting = new MeetingEntity();
        meeting.setId(10L);
        meeting.setTitle("Standup");
        meeting.setCreator(creator);
        meeting.setStartTime(OffsetDateTime.now().plusMinutes(minutes));
        meeting.addParticipant(creator);
        return meeting;
    }

    private static UserEntity user(long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    @Test
    void claimsReminderBeforePushing() {
        MeetingEntity meeting = meetingStartingIn(10, user(1L, "alice"));
        when(meetingRepository.findByStartTimeBetween(any(), any())).thenReturn(List.of(meeting));
        when(reminderSentRepository.existsByMeetingIdAndUserId(10L, 1L)).thenReturn(false);
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(reminderSentRepository.claim(eq(10L), eq(1L), any(OffsetDateTime.class))).thenReturn(1);

        worker.sendDueReminders();

        InOrder order = inOrder(reminderSentRepository, notificationService);
        order.verify(reminderSentRepository).claim(eq(10L), eq(1L), any(OffsetDateTime.class));
        order.verify(notificationService).push(eq(1L), eq("MEETING_REMINDER"), eq("meeting_reminders"),
                eq("MEETING_REMINDER"), anyMap());
    }

    @Test
    void reminderClaimedByAnotherWorkerIsNotPushed() {
        MeetingEntity meeting = meetingStartingIn(10, user(1L, "alice"));
        when(meetingRepository.findByStartTimeBetween(any(), any())).thenReturn(List.of(meeting));
        when(reminderSentRepository.existsByMeetingIdAndUserId(10L, 1L)).thenReturn(false);
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(reminderSentRepository.claim(eq(10L), eq(1L), any(OffsetDateTime.class))).thenReturn(0);

        worker.sendDueReminders();

        verifyNoInteractions(notificationService);
    }

    @Test
    void reminderOutsideLeadWindowIsNotClaimed() {
        MeetingEntity meeting = meetingStartingIn(120, user(1L, "alice"));
        when(meetingRepository.findByStartTimeBetween(any(), any())).thenReturn(List.of(meeting));
        when(reminderSentRepository.existsByMeetingIdAndUserId(10L, 1L)).thenReturn(false);
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        worker.sendDueReminders();

        verify(reminderSentRepository, never()).claim(anyLong(), anyLong(), any());
        verify(notificationService, never()).push(anyLong(), anyString(), anyString(), anyString(), anyMap());
    }
}
