package com.meetingplanner.common.api.dto;

import java.util.List;

public record GroupResponse(
        Long id,
        String groupName,
        Long ownerId,
        boolean isPrivate,
        int memberCount,
        boolean membersCanPost,
        boolean commentsEnabled,
        boolean membersCanCreate,
        // Empty unless the viewer is a member.
        List<GroupMemberResponse> members,
        // The viewer's own "share my schedule with this group" setting; null for non-members.
        Boolean viewerShareSchedule
) {
}