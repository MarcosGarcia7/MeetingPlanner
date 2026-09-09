package com.meetingplanner.common.api.model;

public record TokenPair(String accessToken, String refreshToken, String refreshJti) {}