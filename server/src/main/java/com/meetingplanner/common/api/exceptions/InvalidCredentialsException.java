package com.meetingplanner.common.api.exceptions;

public class InvalidCredentialsException extends RuntimeException {

    private final Long remainingAttempts;

    public InvalidCredentialsException() {
        this(null);
    }

    public InvalidCredentialsException(Long remainingAttempts) {
        super("Invalid credentials");
        this.remainingAttempts = remainingAttempts;
    }

    public Long getRemainingAttempts() {
        return remainingAttempts;
    }
}