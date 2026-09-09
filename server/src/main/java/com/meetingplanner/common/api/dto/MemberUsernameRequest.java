package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberUsernameRequest(
        @NotBlank @Size(max = 50) String username
) {
}