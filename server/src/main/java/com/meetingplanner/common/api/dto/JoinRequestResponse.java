package com.meetingplanner.common.api.dto;

import com.meetingplanner.common.api.model.RequestStatus;

import java.time.OffsetDateTime;

public record JoinRequestResponse(
        Long id,
        String kind,
        RequestStatus status,
        Long requesterId,
        String requesterUsername,
        Long groupId,
        String groupName,
        Long meetingId,
        OffsetDateTime timeSent,
        OffsetDateTime respondedAt
) {
}
