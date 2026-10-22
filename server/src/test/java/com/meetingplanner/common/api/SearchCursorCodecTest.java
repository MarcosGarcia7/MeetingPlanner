package com.meetingplanner.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchCursorCodecTest {

    @Test
    void roundTripsValueAndId() {
        SearchCursorCodec.Cursor decoded = SearchCursorCodec.decode(SearchCursorCodec.encode("alice", 42L));

        assertEquals("alice", decoded.value());
        assertEquals(42L, decoded.id());
    }

    @Test
    void roundTripsValueContainingDelimiter() {
        SearchCursorCodec.Cursor decoded = SearchCursorCodec.decode(SearchCursorCodec.encode("a|b", 7L));

        assertEquals("a|b", decoded.value());
        assertEquals(7L, decoded.id());
    }

    @Test
    void blankCursorDecodesToNull() {
        assertNull(SearchCursorCodec.decode(null));
        assertNull(SearchCursorCodec.decode("  "));
    }

    @Test
    void malformedCursorIsRejected() {
        assertThrows(ResponseStatusException.class, () -> SearchCursorCodec.decode("not-base64!!"));
        assertThrows(ResponseStatusException.class, () -> SearchCursorCodec.decode("bm8tc2VwYXJhdG9y"));
    }
}
