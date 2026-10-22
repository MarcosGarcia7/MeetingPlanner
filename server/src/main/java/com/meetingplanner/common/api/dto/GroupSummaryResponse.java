package com.meetingplanner.common.api.dto;

public record GroupSummaryResponse(
        Long id,
        String groupName,
        Long ownerId,
        boolean isPrivate,
        int memberCount,
        boolean viewerIsMember
) {
}
