package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

public record CreateAccountRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 254, message = "email must be at most 254 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 12, max = 72, message = "password must be between 12 and 72 characters")
        String password,

        @Pattern(regexp = "^[A-Za-z0-9_-]{1,20}$",
                message = "username may only contain letters, digits, hyphens, and underscores, and be at most 20 characters")
        String username,
        @Size(max = 50) String firstName,
        @Size(max = 50) String lastName,
        boolean isPrivate,
        @Size(max = 64, message = "timezone must be at most 64 characters")
        String timezone
) {
    // bcrypt only accepts 72 bytes; @Size counts characters, so multibyte passwords can exceed it.
    @AssertTrue(message = "password must be at most 72 bytes")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}