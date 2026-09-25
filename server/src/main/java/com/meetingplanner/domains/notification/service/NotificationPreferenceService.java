package com.meetingplanner.domains.notification.service;

import com.meetingplanner.common.api.dto.NotificationPreferenceUpdateRequest;
import com.meetingplanner.common.api.dto.NotificationPreferencesResponse;
import com.meetingplanner.domains.notification.model.NotificationPreferenceEntity;
import com.meetingplanner.domains.notification.repository.NotificationPreferenceRepository;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository,
                                         UserRepository userRepository) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse get(long userId) {
        return NotificationPreferencesResponse.from(preferencesFor(userId));
    }

    @Transactional
    public NotificationPreferencesResponse update(long userId, NotificationPreferenceUpdateRequest request) {
        NotificationPreferenceEntity prefs = preferencesFor(userId);

        if (request.emailEnabled() != null) {
            prefs.setEmailEnabled(request.emailEnabled());
        }
        if (request.groupInvites() != null) {
            prefs.setGroupInvites(request.groupInvites());
        }
        if (request.meetingInvites() != null) {
            prefs.setMeetingInvites(request.meetingInvites());
        }
        if (request.meetingReminders() != null) {
            prefs.setMeetingReminders(request.meetingReminders());
        }
        if (request.friendRequests() != null) {
            prefs.setFriendRequests(request.friendRequests());
        }
        if (request.postComments() != null) {
            prefs.setPostComments(request.postComments());
        }
        if (request.groupPosts() != null) {
            prefs.setGroupPosts(request.groupPosts());
        }
        if (request.reminderLeadMinutes() != null) {
            prefs.setReminderLeadMinutes(request.reminderLeadMinutes());
        }

        preferenceRepository.save(prefs);
        return NotificationPreferencesResponse.from(prefs);
    }

    private NotificationPreferenceEntity preferencesFor(long userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
            NotificationPreferenceEntity prefs = new NotificationPreferenceEntity();
            prefs.setUserId(userId);
            prefs.setUser(user);
            return preferenceRepository.save(prefs);
        });
    }
}