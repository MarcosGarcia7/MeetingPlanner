package com.meetingplanner.common.api.exceptions;

// Wrong current password on an authenticated request. Deliberately not 401: the BFF treats 401 as an
// expired access token and would refresh and retry.
public class IncorrectPasswordException extends RuntimeException {

    public IncorrectPasswordException() {
        super("Current password is incorrect");
    }
}
