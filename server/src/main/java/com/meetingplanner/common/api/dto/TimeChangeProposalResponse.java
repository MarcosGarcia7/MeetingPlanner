package com.meetingplanner.common.api.dto;

import com.meetingplanner.common.api.model.ProposalStatus;
import com.meetingplanner.common.api.model.VoteStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record TimeChangeProposalResponse(
        Long id,
        Long meetingId,
        String meetingTitle,
        Long proposedById,
        String proposedByUsername,
        OffsetDateTime proposedStartTime,
        OffsetDateTime proposedEndTime,
        ProposalStatus status,
        OffsetDateTime timeSent,
        OffsetDateTime resolvedAt,
        VoteStatus myVote,
        List<TimeChangeConflict> conflicts
) {
}
