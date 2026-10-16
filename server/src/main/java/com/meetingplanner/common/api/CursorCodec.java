package com.meetingplanner.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

public final class CursorCodec {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private CursorCodec() {
    }

    public static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    public static String encode(OffsetDateTime time, long id) {
        return encodeRaw(time + "|" + id);
    }

    public static String encode(OffsetDateTime time, String kind, long id) {
        return encodeRaw(time + "|" + (kind == null ? "" : kind) + "|" + id);
    }

    public static String encodeId(long id) {
        return encodeRaw(Long.toString(id));
    }

    public static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", -1);
            if (parts.length == 1) {
                return new Cursor(null, null, Long.parseLong(parts[0]));
            }
            if (parts.length == 2) {
                return new Cursor(OffsetDateTime.parse(parts[0]), null, Long.parseLong(parts[1]));
            }
            if (parts.length == 3) {
                String kind = parts[1].isEmpty() ? null : parts[1];
                return new Cursor(OffsetDateTime.parse(parts[0]), kind, Long.parseLong(parts[2]));
            }
            throw new IllegalArgumentException("Malformed cursor");
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }

    private static String encodeRaw(String raw) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public record Cursor(OffsetDateTime time, String kind, long id) {
    }
}
