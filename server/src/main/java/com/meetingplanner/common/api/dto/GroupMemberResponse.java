package com.meetingplanner.common.api.dto;

public record GroupMemberResponse(
        Long userId,
        String username,
        String firstName,
        String lastName,
        String avatarColor,
        boolean isAdmin
) {
}