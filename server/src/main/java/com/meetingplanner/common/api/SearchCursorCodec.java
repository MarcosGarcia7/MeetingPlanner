package com.meetingplanner.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class SearchCursorCodec {

    private SearchCursorCodec() {
    }

    public static String encode(String value, long id) {
        String raw = (value == null ? "" : value) + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separator = raw.lastIndexOf('|');
            if (separator < 0) {
                throw new IllegalArgumentException("Malformed cursor");
            }
            return new Cursor(raw.substring(0, separator), Long.parseLong(raw.substring(separator + 1)));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }

    public record Cursor(String value, long id) {
    }
}
