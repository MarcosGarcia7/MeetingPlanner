package com.meetingplanner.common.api.exceptions;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Email address is not verified");
    }
}