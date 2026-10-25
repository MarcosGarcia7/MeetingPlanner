package com.meetingplanner.common.api.dto;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.meetingplanner.common.api.dto.DtoValidation.violatedPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateAccountRequestTest {

    private static CreateAccountRequest withPassword(String password) {
        return new CreateAccountRequest("alice@example.com", password, "alice", null, null, false, null);
    }

    @Test
    void asciiPasswordAtLimitPasses() {
        assertTrue(violatedPaths(withPassword("a".repeat(72))).isEmpty());
    }

    @Test
    void multibytePasswordAtByteLimitPasses() {
        // 36 two-byte characters = 72 bytes
        assertTrue(violatedPaths(withPassword("é".repeat(36))).isEmpty());
    }

    @Test
    void multibytePasswordOverByteLimitFails() {
        // 72 characters (passes @Size) but 144 bytes
        assertEquals(Set.of("passwordWithinBcryptLimit"), violatedPaths(withPassword("é".repeat(72))));
    }

    @Test
    void emojiPasswordOverByteLimitFails() {
        // 19 four-byte emoji = 38 chars (UTF-16) but 76 bytes
        assertEquals(Set.of("passwordWithinBcryptLimit"), violatedPaths(withPassword("😀".repeat(19))));
    }

    @Test
    void missingPasswordReportsOnlyNotBlank() {
        assertEquals(Set.of("password"), violatedPaths(withPassword(null)));
    }
}
