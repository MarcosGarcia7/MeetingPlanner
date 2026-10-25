package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(max = 50) String firstName,
        @Size(max = 50) String lastName,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "avatarColor must be a hex color like #3B82F6")
        String avatarColor,
        @Size(max = 64, message = "timezone must be at most 64 characters") String timezone,
        Boolean isPrivate,
        Boolean friendsCanViewSchedule,
        @Email @Size(max = 254) String pendingEmail
) {
}