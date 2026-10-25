package com.meetingplanner.common.api.dto;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.meetingplanner.common.api.dto.DtoValidation.violatedPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResetPasswordRequestTest {

    private static final String TOKEN = "a".repeat(64);

    @Test
    void validRequestPasses() {
        assertTrue(violatedPaths(new ResetPasswordRequest(TOKEN, "correct-horse-battery")).isEmpty());
    }

    @Test
    void multibytePasswordOverByteLimitFails() {
        assertEquals(Set.of("newPasswordWithinBcryptLimit"),
                violatedPaths(new ResetPasswordRequest(TOKEN, "é".repeat(72))));
    }

    @Test
    void tokenAtLimitPasses() {
        assertTrue(violatedPaths(new ResetPasswordRequest("a".repeat(128), "correct-horse-battery")).isEmpty());
    }

    @Test
    void oversizedTokenFails() {
        assertEquals(Set.of("token"), violatedPaths(new ResetPasswordRequest("a".repeat(129), "correct-horse-battery")));
    }
}
