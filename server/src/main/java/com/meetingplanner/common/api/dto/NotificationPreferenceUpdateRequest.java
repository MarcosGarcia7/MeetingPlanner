package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record NotificationPreferenceUpdateRequest(
        Boolean emailEnabled,
        Boolean groupInvites,
        Boolean meetingInvites,
        Boolean meetingReminders,
        Boolean meetingUpdates,
        Boolean friendRequests,
        Boolean postComments,
        Boolean groupPosts,
        @Min(value = 5, message = "reminderLeadMinutes must be at least 5")
        @Max(value = 1440, message = "reminderLeadMinutes must be at most 1440")
        Integer reminderLeadMinutes
) {
}