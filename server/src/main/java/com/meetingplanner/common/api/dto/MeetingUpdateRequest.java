package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;


public record MeetingUpdateRequest(
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        @Size(max = 500) String location,
        @Size(max = 500) String description,
        Boolean isPrivate
) {
}