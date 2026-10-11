package com.meetingplanner.common.api.exceptions;

public class AvailabilityNotSetException extends RuntimeException {

    public AvailabilityNotSetException() { super("Availability is not set"); }
}
