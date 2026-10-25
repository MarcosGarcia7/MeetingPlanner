package com.meetingplanner.common.api.dto;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.meetingplanner.common.api.dto.DtoValidation.violatedPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserProfileUpdateRequestTest {

    private static UserProfileUpdateRequest withColor(String avatarColor) {
        return new UserProfileUpdateRequest(null, null, avatarColor, null, null, null, null);
    }

    @Test
    void hexColorPasses() {
        assertTrue(violatedPaths(withColor("#3B82F6")).isEmpty());
        assertTrue(violatedPaths(withColor("#3b82f6")).isEmpty());
    }

    @Test
    void nullColorPasses() {
        assertTrue(violatedPaths(withColor(null)).isEmpty());
    }

    @Test
    void nonHexColorsFail() {
        for (String color : new String[]{"red", "3B82F6", "#3B82F", "#3B82F6A", "#GGGGGG", "#3B82F6;x", ""}) {
            assertEquals(Set.of("avatarColor"), violatedPaths(withColor(color)), color);
        }
    }
}
