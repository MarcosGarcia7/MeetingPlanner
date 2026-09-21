package com.meetingplanner.common.api.dto;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean isPrivate,
        boolean friendsCanViewPrivate,
        boolean emailVerified
) {
}