package com.meetingplanner.common.api.dto;

import com.meetingplanner.common.api.model.ShareLinkJoinMode;
import com.meetingplanner.common.api.model.TargetKind;

import java.time.OffsetDateTime;

// What a share link's landing page shows. joinMode is the effective mode (a REQUEST link on a public group
// joins directly). The meeting fields are null for group links. viewerIsMember is null for anonymous viewers.
public record ShareLinkPreviewResponse(
        TargetKind kind,
        Long groupId,
        String groupName,
        Long meetingId,
        String meetingTitle,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String location,
        String createdByUsername,
        ShareLinkJoinMode joinMode,
        Boolean viewerIsMember
) {
}
