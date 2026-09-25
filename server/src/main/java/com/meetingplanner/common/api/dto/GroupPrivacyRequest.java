package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotNull;

public record GroupPrivacyRequest(
        @NotNull(message = "shareSchedule is required")
        Boolean shareSchedule
) {
}