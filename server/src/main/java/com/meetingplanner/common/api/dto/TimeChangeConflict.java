package com.meetingplanner.common.api.dto;

import java.time.OffsetDateTime;

public record TimeChangeConflict(
        Long meetingId,
        String title,
        OffsetDateTime startTime
) {
}
