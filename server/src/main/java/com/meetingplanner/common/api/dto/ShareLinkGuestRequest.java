package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShareLinkGuestRequest(
        @NotBlank @Email @Size(max = 254) String email
) {
}
