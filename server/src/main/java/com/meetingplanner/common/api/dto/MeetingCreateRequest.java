package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record MeetingCreateRequest(
        Long groupId,
        @NotNull OffsetDateTime startTime,
        OffsetDateTime endTime,
        @Size(max = 500) String location,
        @Size(max = 500) String description,
        List<@Size(max = 50) String> externalParticipantNames
) {
}