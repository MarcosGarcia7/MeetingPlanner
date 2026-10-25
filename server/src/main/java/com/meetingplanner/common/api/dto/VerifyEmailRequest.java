package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotBlank(message = "token is required")
        @Size(max = 128, message = "token must be at most 128 characters")
        String token
) {
}