package com.meetingplanner.common.api;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MeetingTimesTest {

    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");

    private static OffsetDateTime at(String iso) {
        return OffsetDateTime.parse(iso);
    }

    @Test
    void sameDaySamePeriodSharesTheSuffix() {
        assertEquals("Tue, Oct 27, 2026, 2:00 – 3:30 PM CDT",
                MeetingTimes.format(at("2026-10-27T19:00:00Z"), at("2026-10-27T20:30:00Z"), CHICAGO));
    }

    @Test
    void sameDayAcrossNoonShowsBothPeriods() {
        assertEquals("Tue, Oct 27, 2026, 11:00 AM – 2:00 PM CDT",
                MeetingTimes.format(at("2026-10-27T16:00:00Z"), at("2026-10-27T19:00:00Z"), CHICAGO));
    }

    @Test
    void crossingMidnightShowsTheEndDay() {
        assertEquals("Tue, Oct 27, 2026, 11:00 PM – Wed, Oct 28, 12:30 AM CDT",
                MeetingTimes.format(at("2026-10-28T04:00:00Z"), at("2026-10-28T05:30:00Z"), CHICAGO));
    }

    @Test
    void withoutEndShowsOnlyTheStart() {
        assertEquals("Tue, Oct 27, 2026, 7:00 PM UTC",
                MeetingTimes.format(at("2026-10-27T19:00:00Z"), null, MeetingTimes.zoneFor((String) null)));
    }

    @Test
    void usesStandardTimeAfterDaylightSavingEnds() {
        assertEquals("Tue, Nov 3, 2026, 1:00 PM CST",
                MeetingTimes.format(at("2026-11-03T19:00:00Z"), null, CHICAGO));
    }

    @Test
    void zoneForSkipsMissingAndInvalidZones() {
        assertEquals(CHICAGO, MeetingTimes.zoneFor(null, " ", "Not/AZone", "America/Chicago"));
    }

    @Test
    void zoneForFallsBackToUtc() {
        assertEquals(ZoneId.of("UTC"), MeetingTimes.zoneFor(null, "Not/AZone"));
    }
}
