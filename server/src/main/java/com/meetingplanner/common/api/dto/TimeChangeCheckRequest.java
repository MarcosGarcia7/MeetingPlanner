package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record TimeChangeCheckRequest(
        @NotNull OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}
