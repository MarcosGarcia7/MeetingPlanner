package com.meetingplanner.common.api.exceptions;

/** The acting user would be double-booked: the times overlap another meeting on their schedule. */
public class ScheduleConflictException extends RuntimeException {

    public ScheduleConflictException(String message) {
        super(message);
    }
}
