package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.Size;

public record GroupUpdateRequest(
        @Size(max = 50) String groupName,
        Boolean isPrivate,
        Boolean membersCanPost,
        Boolean commentsEnabled
) {
}