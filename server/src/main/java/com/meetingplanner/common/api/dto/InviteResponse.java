package com.meetingplanner.common.api.dto;

import com.meetingplanner.common.api.model.InviteStatus;

public record InviteResponse(
        Long id,
        String kind,
        InviteStatus status,
        Long senderId,
        String senderUsername,
        Long recipientId,
        String recipientEmail,
        Long groupId,
        Long meetingId
) {
}