package com.meetingplanner.common.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record MeetingDraftCheckRequest(
        @NotNull OffsetDateTime startTime,
        OffsetDateTime endTime,
        @Valid @Size(max = 50) List<MeetingInviteRequest> recipients
) {
}
