package com.meetingplanner.common.api.model;

// The `when` filter on GET /api/meetings. UPCOMING: not over yet (in progress counts), soonest first.
// PAST: already ended, latest first.
public enum MeetingTimeframe {
    UPCOMING,
    PAST
}
