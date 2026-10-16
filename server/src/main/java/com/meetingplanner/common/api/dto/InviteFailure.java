package com.meetingplanner.common.api.dto;

public record InviteFailure(
        String username,
        String email,
        String reason
) {
}