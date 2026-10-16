package com.meetingplanner.common.api.dto;

import java.util.List;

public record InviteCheckResponse(
        List<InviteCheckResult> recipients
) {
}
