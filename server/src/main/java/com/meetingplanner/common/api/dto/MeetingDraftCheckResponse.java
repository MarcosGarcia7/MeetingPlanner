package com.meetingplanner.common.api.dto;

import java.util.List;

public record MeetingDraftCheckResponse(
        List<String> conflicts,
        List<String> warnings,
        List<InviteCheckResult> recipients
) {
}
