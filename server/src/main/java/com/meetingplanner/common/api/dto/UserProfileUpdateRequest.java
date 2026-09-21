package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(max = 50) String firstName,
        @Size(max = 50) String lastName,
        Boolean isPrivate,
        Boolean friendsCanViewPrivate,
        @Email @Size(max = 254) String pendingEmail
) {
}