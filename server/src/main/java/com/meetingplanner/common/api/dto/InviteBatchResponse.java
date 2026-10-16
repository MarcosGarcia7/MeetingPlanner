package com.meetingplanner.common.api.dto;

import java.util.List;

public record InviteBatchResponse(
        List<InviteResponse> invited,
        List<InviteFailure> failed
) {
}