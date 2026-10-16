package com.meetingplanner.common.api.dto;

import com.meetingplanner.domains.notification.model.NotificationPreferenceEntity;

public record NotificationPreferencesResponse(
        boolean emailEnabled,
        boolean groupInvites,
        boolean meetingInvites,
        boolean meetingReminders,
        boolean meetingUpdates,
        boolean friendRequests,
        boolean postComments,
        boolean groupPosts,
        int reminderLeadMinutes
) {

    public static NotificationPreferencesResponse from(NotificationPreferenceEntity prefs) {
        return new NotificationPreferencesResponse(
                prefs.isEmailEnabled(),
                prefs.isGroupInvites(),
                prefs.isMeetingInvites(),
                prefs.isMeetingReminders(),
                prefs.isMeetingUpdates(),
                prefs.isFriendRequests(),
                prefs.isPostComments(),
                prefs.isGroupPosts(),
                prefs.getReminderLeadMinutes()
        );
    }
}