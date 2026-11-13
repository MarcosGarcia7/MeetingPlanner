package com.meetingplanner.common.api.dto;

import com.meetingplanner.common.api.model.ShareLinkJoinMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// expiresInDays / maxUses null = never expires / unlimited uses.
public record CreateGroupShareLinkRequest(
        @NotNull ShareLinkJoinMode joinMode,
        @Min(1) @Max(365) Integer expiresInDays,
        @Min(1) @Max(1000) Integer maxUses
) {
}
