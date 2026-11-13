package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// expiresInDays / maxUses null = never expires / unlimited uses.
public record CreateMeetingShareLinkRequest(
        @Min(1) @Max(365) Integer expiresInDays,
        @Min(1) @Max(1000) Integer maxUses
) {
}
