package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginUsernameRequest(
        @NotBlank(message = "username is required")
        @Size(max = 20, message = "username must be at most 20 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(max = 128, message = "password must be at most 128 characters")
        String password) {}