package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotBlank(message = "body is required")
        @Size(max = 10000, message = "body must be at most 10000 characters")
        String body,

        Boolean commentsEnabled
) {
}