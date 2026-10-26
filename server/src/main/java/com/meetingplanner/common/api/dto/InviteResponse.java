package com.meetingplanner.common.api.dto;

import com.meetingplanner.common.api.model.InviteStatus;

import java.util.List;

public record InviteResponse(
        Long id,
        String kind,
        InviteStatus status,
        Long senderId,
        String senderUsername,
        Long recipientId,
        String recipientEmail,
        Long groupId,
        String groupName,
        Long meetingId,
        String meetingTitle,
        List<String> warnings
) {
}