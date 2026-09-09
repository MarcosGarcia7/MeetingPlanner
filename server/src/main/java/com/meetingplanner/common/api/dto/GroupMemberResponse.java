package com.meetingplanner.common.api.dto;

public record GroupMemberResponse(
        Long userId,
        String username,
        boolean isAdmin
) {
}