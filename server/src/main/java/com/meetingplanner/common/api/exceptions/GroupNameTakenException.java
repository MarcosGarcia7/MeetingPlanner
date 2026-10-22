package com.meetingplanner.common.api.exceptions;

public class GroupNameTakenException extends RuntimeException {

    public GroupNameTakenException(String groupName) {
        super("Group name is already taken");
    }
}
