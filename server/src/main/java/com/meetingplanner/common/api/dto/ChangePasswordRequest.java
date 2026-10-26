package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

public record ChangePasswordRequest(
        @NotBlank(message = "currentPassword is required")
        @Size(max = 72, message = "currentPassword must be at most 72 characters")
        String currentPassword,

        @NotBlank(message = "password is required")
        @Size(min = 12, max = 72, message = "password must be between 12 and 72 characters")
        String newPassword
) {
    @AssertTrue(message = "password must be at most 72 bytes")
    public boolean isNewPasswordWithinBcryptLimit() {
        return newPassword == null || newPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @AssertTrue(message = "new password must be different from the current password")
    public boolean isNewPasswordDifferent() {
        return newPassword == null || !newPassword.equals(currentPassword);
    }
}
