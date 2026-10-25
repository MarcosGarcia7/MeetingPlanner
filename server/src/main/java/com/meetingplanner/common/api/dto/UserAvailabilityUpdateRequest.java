package com.meetingplanner.common.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

// Each day is a list of [startMinute, endMinute) windows, 0–1440. A null day is left unchanged.
public record UserAvailabilityUpdateRequest(
        List<@NotNull @Size(min = 2, max = 2, message = "window must be [start, end]")
             List<@NotNull @Min(0) @Max(1440) Integer>> sunday,
        List<@NotNull @Size(min = 2, max = 2, message = "window must be [start, end]")
             List<@NotNull @Min(0) @Max(1440) Integer>> monday,
        List<@NotNull @Size(min = 2, max = 2, message = "window must be [start, end]")
             List<@NotNull @Min(0) @Max(1440) Integer>> tuesday,
        List<@NotNull @Size(min = 2, max = 2, message = "window must be [start, end]")
             List<@NotNull @Min(0) @Max(1440) Integer>> wednesday,
        List<@NotNull @Size(min = 2, max = 2, message = "window must be [start, end]")
             List<@NotNull @Min(0) @Max(1440) Integer>> thursday,
        List<@NotNull @Size(min = 2, max = 2, message = "window must be [start, end]")
             List<@NotNull @Min(0) @Max(1440) Integer>> friday,
        List<@NotNull @Size(min = 2, max = 2, message = "window must be [start, end]")
             List<@NotNull @Min(0) @Max(1440) Integer>> saturday,
        @NotNull(message = "availabilitySet is required")
        Boolean availabilitySet
) {
    // Reported as field "windowsOrdered". Malformed windows are skipped; the element constraints
    // above already report those.
    @AssertTrue(message = "each window's end must be after its start")
    public boolean isWindowsOrdered() {
        return Stream.of(sunday, monday, tuesday, wednesday, thursday, friday, saturday)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(w -> w != null && w.size() == 2 && w.get(0) != null && w.get(1) != null)
                .allMatch(w -> w.get(1) > w.get(0));
    }
}
