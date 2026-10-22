package com.meetingplanner.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchQueryTest {

    @Test
    void trimsAndLowercasesThePattern() {
        SearchQuery query = SearchQuery.parse("  Alice  ");

        assertEquals("Alice", query.value());
        assertEquals("%alice%", query.pattern());
    }

    @Test
    void escapesLikeWildcardsAndEscapeCharacter() {
        SearchQuery query = SearchQuery.parse(" 50% off_! ");

        assertEquals("%50!% off!_!!%", query.pattern());
    }

    @Test
    void rejectsBlankQuery() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> SearchQuery.parse("   "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void acceptsQueryAtLimit() {
        SearchQuery query = SearchQuery.parse("a".repeat(SearchQuery.MAX_LENGTH));

        assertEquals(SearchQuery.MAX_LENGTH, query.value().length());
    }

    @Test
    void rejectsQueryLongerThanLimit() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> SearchQuery.parse("a".repeat(SearchQuery.MAX_LENGTH + 1)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
