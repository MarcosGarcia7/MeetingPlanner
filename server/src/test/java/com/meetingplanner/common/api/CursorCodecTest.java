package com.meetingplanner.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CursorCodecTest {

    private final OffsetDateTime time = OffsetDateTime.parse("2026-10-12T10:15:30Z");

    @Test
    void roundTripsTimeAndId() {
        String encoded = CursorCodec.encode(time, 42L);
        CursorCodec.Cursor decoded = CursorCodec.decode(encoded);

        assertEquals(time, decoded.time());
        assertNull(decoded.kind());
        assertEquals(42L, decoded.id());
    }

    @Test
    void roundTripsTimeKindAndId() {
        String encoded = CursorCodec.encode(time, "MEETING", 7L);
        CursorCodec.Cursor decoded = CursorCodec.decode(encoded);

        assertEquals(time, decoded.time());
        assertEquals("MEETING", decoded.kind());
        assertEquals(7L, decoded.id());
    }

    @Test
    void roundTripsIdOnly() {
        CursorCodec.Cursor decoded = CursorCodec.decode(CursorCodec.encodeId(99L));

        assertNull(decoded.time());
        assertNull(decoded.kind());
        assertEquals(99L, decoded.id());
    }

    @Test
    void blankCursorDecodesToNull() {
        assertNull(CursorCodec.decode(null));
        assertNull(CursorCodec.decode("  "));
    }

    @Test
    void malformedCursorIsRejected() {
        assertThrows(ResponseStatusException.class, () -> CursorCodec.decode("not-base64!!"));
    }

    @Test
    void clampsSize() {
        assertEquals(CursorCodec.DEFAULT_PAGE_SIZE, CursorCodec.clampSize(0));
        assertEquals(CursorCodec.DEFAULT_PAGE_SIZE, CursorCodec.clampSize(-5));
        assertEquals(15, CursorCodec.clampSize(15));
        assertEquals(CursorCodec.MAX_PAGE_SIZE, CursorCodec.clampSize(500));
    }
}
