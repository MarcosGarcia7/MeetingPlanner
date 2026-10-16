package com.meetingplanner.common.api.dto;

import java.time.OffsetDateTime;

public record PendingTimeChange(
        Long proposalId,
        Long proposedById,
        OffsetDateTime proposedStartTime,
        OffsetDateTime proposedEndTime
) {
}
