package com.meetingplanner.common.api.dto;

public record UserProfileResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        boolean isPrivate
) {
}