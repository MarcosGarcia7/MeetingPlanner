package com.meetingplanner.common.api.dto;

import java.util.List;

public record InviteCheckResult(
        String username,
        String email,
        List<String> warnings
) {
}
