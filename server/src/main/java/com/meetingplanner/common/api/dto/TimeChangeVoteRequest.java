package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.NotNull;

public record TimeChangeVoteRequest(
        @NotNull Boolean approve
) {
}
