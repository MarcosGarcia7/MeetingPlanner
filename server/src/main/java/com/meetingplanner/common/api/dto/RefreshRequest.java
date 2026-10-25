package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank(message = "refreshToken is required")
        @Size(max = 4096, message = "refreshToken must be at most 4096 characters")
        String refreshToken) {}