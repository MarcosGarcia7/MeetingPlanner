package com.meetingplanner.common.api.dto;

import java.time.OffsetDateTime;

public record MeetingSummaryResponse(
        Long id,
        Long groupId,
        Long creatorId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String location,
        String description,
        boolean isPrivate,
        int participantCount
) {
}
