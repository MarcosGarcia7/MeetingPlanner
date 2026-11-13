package com.meetingplanner.common.api.dto;

import com.meetingplanner.common.api.model.ShareLinkJoinMode;

import java.time.OffsetDateTime;

// joinMode is null for meeting links, which always join directly. usable is false once the link has expired,
// hit its use limit, or its creator lost the right to share (a manager should then reset it).
public record ShareLinkResponse(
        String token,
        String url,
        ShareLinkJoinMode joinMode,
        OffsetDateTime expiresAt,
        Integer maxUses,
        int useCount,
        OffsetDateTime createdAt,
        String createdByUsername,
        boolean usable
) {
}
