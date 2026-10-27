package com.meetingplanner.common.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MeetingResponse(
        Long id,
        Long groupId,
        String groupName,
        Long creatorId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String location,
        String title,
        boolean isPrivate,
        List<String> externalParticipantNames,
        List<ParticipantResponse> participants,
        List<String> warnings,
        PendingTimeChange pendingTimeChange,
        // Whether the viewer may edit, delete, invite to, and remove people from the meeting: its creator,
        // or an owner/admin of its group.
        boolean canManage
) {
}