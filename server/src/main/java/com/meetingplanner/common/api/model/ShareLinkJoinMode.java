package com.meetingplanner.common.api.model;

// What a group share link does for a signed-in user: join straight away, or send a join request.
// Meeting links always join directly.
public enum ShareLinkJoinMode {
    DIRECT,
    REQUEST
}
