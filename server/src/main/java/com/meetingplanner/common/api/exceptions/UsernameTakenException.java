package com.meetingplanner.common.api.exceptions;

public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Username is already taken");
    }
}