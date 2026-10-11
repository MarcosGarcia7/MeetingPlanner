package com.meetingplanner.common.api.dto;

public record UserAvailabilityResponse(
        Long id,
        int[][] sunday,
        int[][] monday,
        int[][] tuesday,
        int[][] wednesday,
        int[][] thursday,
        int[][] friday,
        int[][] saturday,
        boolean availabilitySet
) {
}
