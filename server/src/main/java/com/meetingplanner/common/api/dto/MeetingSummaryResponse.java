package com.meetingplanner.common.api.dto;

import java.time.OffsetDateTime;

public record MeetingSummaryResponse(
        Long id,
        Long groupId,
        String groupName,
        Long creatorId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String location,
        String title,
        boolean isPrivate,
        int participantCount
) {
}
