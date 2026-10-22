package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record MeetingCreateRequest(
        @NotNull OffsetDateTime startTime,
        OffsetDateTime endTime,
        @Size(max = 500) String location,
        @NotBlank @Size(max = 100) String title,
        Boolean isPrivate
) {
}