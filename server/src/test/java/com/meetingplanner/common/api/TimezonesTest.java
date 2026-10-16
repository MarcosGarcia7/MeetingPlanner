package com.meetingplanner.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimezonesTest {

    @Test
    void resolvesIanaZone() {
        assertEquals("America/New_York", Timezones.resolve("America/New_York"));
    }

    @Test
    void resolvesOffsetZone() {
        assertEquals("+05:30", Timezones.resolve("+05:30"));
    }

    @Test
    void trimsWhitespace() {
        assertEquals("Europe/Paris", Timezones.resolve("  Europe/Paris  "));
    }

    @Test
    void blankBecomesNull() {
        assertNull(Timezones.resolve(null));
        assertNull(Timezones.resolve(""));
        assertNull(Timezones.resolve("   "));
    }

    @Test
    void invalidZoneIsRejected() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> Timezones.resolve("Not/AZone"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
