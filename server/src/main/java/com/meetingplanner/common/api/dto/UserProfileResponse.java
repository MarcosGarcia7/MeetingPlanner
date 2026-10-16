package com.meetingplanner.common.api.dto;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String avatarColor,
        String timezone,
        boolean isPrivate,
        boolean friendsCanViewSchedule,
        boolean emailVerified
) {
}