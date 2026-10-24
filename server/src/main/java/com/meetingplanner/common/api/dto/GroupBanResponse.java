package com.meetingplanner.common.api.dto;

import java.time.OffsetDateTime;

public record GroupBanResponse(
        Long userId,
        String username,
        Long bannedById,
        String bannedByUsername,
        boolean bannedByOwner,
        OffsetDateTime bannedAt
) {
}
