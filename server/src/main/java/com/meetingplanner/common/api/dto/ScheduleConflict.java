package com.meetingplanner.common.api.dto;

import java.time.OffsetDateTime;

/**
 * A meeting on the viewer's schedule that some other time (a proposed time change, an invite) overlaps.
 * {@code createdByViewer} tells the client whether the viewer can leave it (participant) or only delete it
 * (creator).
 */
public record ScheduleConflict(
        Long meetingId,
        String title,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        boolean createdByViewer
) {
}
