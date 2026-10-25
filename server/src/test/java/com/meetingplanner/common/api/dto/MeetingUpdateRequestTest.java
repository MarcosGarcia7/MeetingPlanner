package com.meetingplanner.common.api.dto;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static com.meetingplanner.common.api.dto.DtoValidation.violatedPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingUpdateRequestTest {

    private static MeetingUpdateRequest withTitle(String title) {
        return new MeetingUpdateRequest(null, null, null, title, null);
    }

    @Test
    void omittedTitlePasses() {
        // A PATCH that only moves the time doesn't resend the title.
        assertTrue(violatedPaths(new MeetingUpdateRequest(
                OffsetDateTime.parse("2030-01-01T10:00:00Z"), null, null, null, null)).isEmpty());
    }

    @Test
    void titleChangePasses() {
        assertTrue(violatedPaths(withTitle("Standup")).isEmpty());
        assertTrue(violatedPaths(withTitle("Standup\nwith notes")).isEmpty());
    }

    @Test
    void blankTitleFails() {
        assertEquals(Set.of("title"), violatedPaths(withTitle("")));
        assertEquals(Set.of("title"), violatedPaths(withTitle("   ")));
    }

    @Test
    void oversizedTitleFails() {
        assertEquals(Set.of("title"), violatedPaths(withTitle("a".repeat(101))));
    }
}
