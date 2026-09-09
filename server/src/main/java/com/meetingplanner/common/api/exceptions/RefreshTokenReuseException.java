package com.meetingplanner.common.api.exceptions;

public class RefreshTokenReuseException extends RuntimeException {

    public RefreshTokenReuseException() {
        super("Refresh token reuse detected");
    }
}