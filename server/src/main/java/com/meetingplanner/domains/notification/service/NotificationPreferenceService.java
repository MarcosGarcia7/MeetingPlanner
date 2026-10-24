package com.meetingplanner.domains.notification.service;

import com.meetingplanner.common.api.dto.NotificationPreferenceUpdateRequest;
import com.meetingplanner.common.api.dto.NotificationPreferencesResponse;
import com.meetingplanner.domains.notification.model.NotificationPreferenceEntity;
import com.meetingplanner.domains.notification.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    // Not read-only: the first read creates the default row.
    @Transactional
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
        if (request.meetingUpdates() != null) {
            prefs.setMeetingUpdates(request.meetingUpdates());
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

        return NotificationPreferencesResponse.from(preferenceRepository.save(prefs));
    }

    private NotificationPreferenceEntity preferencesFor(long userId) {
        return preferenceRepository.findOrCreate(userId);
    }
}