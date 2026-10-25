package com.meetingplanner.common.api.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.meetingplanner.common.api.dto.DtoValidation.violatedPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAvailabilityUpdateRequestTest {

    private static UserAvailabilityUpdateRequest monday(List<List<Integer>> windows) {
        return new UserAvailabilityUpdateRequest(null, windows, null, null, null, null, null, true);
    }

    @Test
    void validWindowsPass() {
        assertTrue(violatedPaths(monday(List.of(List.of(540, 720), List.of(780, 1020)))).isEmpty());
    }

    @Test
    void fullDayBoundsPass() {
        assertTrue(violatedPaths(monday(List.of(List.of(0, 1440)))).isEmpty());
    }

    @Test
    void allDaysNullPasses() {
        assertTrue(violatedPaths(new UserAvailabilityUpdateRequest(
                null, null, null, null, null, null, null, false)).isEmpty());
    }

    @Test
    void reversedWindowFails() {
        assertEquals(Set.of("windowsOrdered"), violatedPaths(monday(List.of(List.of(1020, 540)))));
    }

    @Test
    void emptyWindowFails() {
        assertEquals(Set.of("windowsOrdered"), violatedPaths(monday(List.of(List.of(540, 540)))));
    }

    @Test
    void outOfRangeMinutesFail() {
        Set<String> paths = violatedPaths(monday(List.of(List.of(-5, 1500))));
        assertEquals(2, paths.size());
        assertTrue(paths.stream().allMatch(p -> p.startsWith("monday[0]")));
    }

    @Test
    void wrongWindowSizeFails() {
        Set<String> paths = violatedPaths(monday(List.of(List.of(60))));
        assertEquals(1, paths.size());
        assertTrue(paths.iterator().next().startsWith("monday[0]"));
    }

    @Test
    void nullMinuteFails() {
        Set<String> paths = violatedPaths(monday(List.of(Arrays.asList(null, 60))));
        assertEquals(1, paths.size());
        assertTrue(paths.iterator().next().startsWith("monday[0]"));
    }

    @Test
    void nullWindowFails() {
        Set<String> paths = violatedPaths(monday(Arrays.asList((List<Integer>) null)));
        assertEquals(1, paths.size());
        assertTrue(paths.iterator().next().startsWith("monday[0]"));
    }

    @Test
    void missingAvailabilitySetFails() {
        assertEquals(Set.of("availabilitySet"), violatedPaths(new UserAvailabilityUpdateRequest(
                null, null, null, null, null, null, null, null)));
    }
}
