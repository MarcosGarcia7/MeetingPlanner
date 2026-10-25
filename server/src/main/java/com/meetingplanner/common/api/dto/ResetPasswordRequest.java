package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

public record ResetPasswordRequest(
        @NotBlank(message = "token is required")
        @Size(max = 128, message = "token must be at most 128 characters")
        String token,

        @NotBlank(message = "password is required")
        @Size(min = 12, max = 72, message = "password must be between 12 and 72 characters")
        String newPassword
) {
    // bcrypt only accepts 72 bytes; @Size counts characters, so multibyte passwords can exceed it.
    @AssertTrue(message = "password must be at most 72 bytes")
    public boolean isNewPasswordWithinBcryptLimit() {
        return newPassword == null || newPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}