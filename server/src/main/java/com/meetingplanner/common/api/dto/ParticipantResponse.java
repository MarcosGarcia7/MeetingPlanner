package com.meetingplanner.common.api.dto;

public record ParticipantResponse(
        Long userId,
        String username
) {
}