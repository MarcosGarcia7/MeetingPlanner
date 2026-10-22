package com.meetingplanner.common.api.dto;

public record UserSearchResult(
        Long id,
        String username,
        String firstName,
        String lastName,
        String avatarColor
) {
}
