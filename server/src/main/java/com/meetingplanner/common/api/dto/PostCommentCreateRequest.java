package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCommentCreateRequest(
        @NotBlank(message = "body is required")
        @Size(max = 2000, message = "body must be at most 2000 characters")
        String body
) {
}