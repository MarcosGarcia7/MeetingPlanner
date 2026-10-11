package com.meetingplanner.common.api.dto;

public record UserAvailabilityUpdateRequest(
        Integer[][] sunday,
        Integer[][] monday,
        Integer[][] tuesday,
        Integer[][] wednesday,
        Integer[][] thursday,
        Integer[][] friday,
        Integer[][] saturday,
        boolean availabilitySet
) {
}
