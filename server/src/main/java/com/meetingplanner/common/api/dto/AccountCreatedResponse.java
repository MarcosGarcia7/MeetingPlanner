package com.meetingplanner.common.api.dto;

public record AccountCreatedResponse(
        String email,
        String message
) {}