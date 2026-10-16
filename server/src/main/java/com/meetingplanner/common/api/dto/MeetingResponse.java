package com.meetingplanner.common.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MeetingResponse(
        Long id,
        Long groupId,
        Long creatorId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String location,
        String description,
        boolean isPrivate,
        List<String> externalParticipantNames,
        List<ParticipantResponse> participants,
        List<String> warnings,
        PendingTimeChange pendingTimeChange
) {
}