package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
        @NotBlank(message = "currentPassword is required")
        @Size(max = 72, message = "currentPassword must be at most 72 characters")
        String currentPassword
) {
}
