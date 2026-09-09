package com.meetingplanner.dto;

import lombok.Value;
import java.time.Instant;

@Value
public class ApiErrorResponse {
    Instant timestamp = Instant.now();
    int status;
    String error;
    String message;
    String path;
}
