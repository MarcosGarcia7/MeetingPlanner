package com.meetingplanner.common.api.dto;

import com.meetingplanner.common.api.model.ShareLinkJoinOutcome;
import com.meetingplanner.common.api.model.TargetKind;

// requestId is set only when the outcome is REQUESTED.
public record ShareLinkJoinResponse(
        ShareLinkJoinOutcome outcome,
        TargetKind kind,
        Long groupId,
        Long meetingId,
        Long requestId
) {
}
