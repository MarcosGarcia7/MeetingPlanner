package com.meetingplanner.common.api.dto;

import com.meetingplanner.common.api.model.GroupViewerStatus;

public record GroupSummaryResponse(
        Long id,
        String groupName,
        Long ownerId,
        boolean isPrivate,
        int memberCount,
        int friendMemberCount,
        GroupViewerStatus viewerStatus,
        Long pendingRequestId,
        Long pendingInviteId,
        // Whether the viewer may schedule meetings in the group.
        boolean canCreateMeetings
) {
}
