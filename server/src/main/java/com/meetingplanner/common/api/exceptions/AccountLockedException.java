package com.meetingplanner.common.api.exceptions;

public class AccountLockedException extends RuntimeException {

    private final long secondsLeft;

    public AccountLockedException(String reason, long secondsLeft) {
        super(reason);
        this.secondsLeft = secondsLeft;
    }

    public long getSecondsLeft() {
        return secondsLeft;
    }
}