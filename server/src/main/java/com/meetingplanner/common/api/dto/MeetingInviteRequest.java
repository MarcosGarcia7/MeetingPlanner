package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record MeetingInviteRequest(
        @Size(max = 20) String username,
        @Email @Size(max = 254) String email
) {
    public boolean hasUsername() {
        return username != null && !username.isBlank();
    }

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }
}
