package com.meetingplanner.common.api.dto;

import com.meetingplanner.common.api.model.InviteStatus;

import java.time.OffsetDateTime;
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
        OffsetDateTime meetingStartTime,
        OffsetDateTime meetingEndTime,
        String meetingLocation,
        // Heads-ups for the sender (e.g. the recipient may be busy); set on invites just sent.
        List<String> warnings,
        // The recipient's meetings this meeting overlaps; set on meeting invites listed for their recipient.
        List<ScheduleConflict> conflicts
) {
}