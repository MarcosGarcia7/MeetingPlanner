package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GroupUpdateRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "groupName cannot be blank")
        @Size(max = 50) String groupName,
        Boolean isPrivate,
        Boolean membersCanPost,
        Boolean commentsEnabled,
        Boolean membersCanCreate
) {
}