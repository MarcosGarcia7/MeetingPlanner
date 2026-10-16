package com.meetingplanner.common.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MeetingInviteBatchRequest(
        @Valid @NotEmpty @Size(max = 50) List<MeetingInviteRequest> recipients
) {
}