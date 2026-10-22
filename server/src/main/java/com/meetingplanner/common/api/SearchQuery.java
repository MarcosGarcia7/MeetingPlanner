package com.meetingplanner.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

public final class SearchQuery {

    public static final int MAX_LENGTH = 500;

    private final String value;
    private final String pattern;

    private SearchQuery(String value, String pattern) {
        this.value = value;
        this.pattern = pattern;
    }

    public static SearchQuery parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query is required");
        }
        String value = raw.trim();
        if (value.length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Search query must be " + MAX_LENGTH + " characters or fewer");
        }
        String escaped = value.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return new SearchQuery(value, "%" + escaped + "%");
    }

    public String value() {
        return value;
    }

    public String pattern() {
        return pattern;
    }
}
