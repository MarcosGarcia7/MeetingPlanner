package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "token is required")
        String token,

        @NotBlank(message = "password is required")
        @Size(min = 12, max = 72, message = "password must be between 12 and 72 characters")
        String newPassword
) {
}