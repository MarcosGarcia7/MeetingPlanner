package com.meetingplanner.common.api.model;

// Whether an invite or join request is for a group or a meeting; the `kind` filter on their list endpoints.
public enum TargetKind {
    GROUP,
    MEETING
}
