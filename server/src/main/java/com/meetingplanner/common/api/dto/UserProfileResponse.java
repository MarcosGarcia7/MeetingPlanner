package com.meetingplanner.common.api.dto;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String avatarColor,
        boolean isPrivate,
        boolean friendsCanViewPrivate,
        boolean emailVerified
) {
}