package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotNull;

public record CommentsEnabledRequest(
        @NotNull(message = "commentsEnabled is required")
        Boolean commentsEnabled
) {
}