package com.meetingplanner.common.api.dto;

import java.util.List;

public record GroupResponse(
        Long id,
        String groupName,
        Long ownerId,
        boolean isPrivate,
        int memberCount,
        List<GroupMemberResponse> members
) {
}