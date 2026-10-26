package com.meetingplanner.common.api.dto;

import com.meetingplanner.common.api.model.UserRelationship;

public record UserSearchResult(
        Long id,
        String username,
        String firstName,
        String lastName,
        String avatarColor,
        UserRelationship relationship
) {
}
