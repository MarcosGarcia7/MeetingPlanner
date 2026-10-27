package com.meetingplanner.common.api.model;

// The user's side of a time change proposal; the `role` filter on GET /api/meetings/time-changes.
public enum ProposalRole {
    PROPOSER,
    VOTER
}
