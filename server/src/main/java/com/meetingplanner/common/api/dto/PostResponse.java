package com.meetingplanner.common.api.dto;

import com.meetingplanner.domains.group.model.GroupPostEntity;

import java.time.OffsetDateTime;

public record PostResponse(
        Long id,
        Long groupId,
        String groupName,
        Long authorId,
        String authorUsername,
        String body,
        boolean commentsEnabled,
        OffsetDateTime createdAt
) {

    public static PostResponse from(GroupPostEntity post) {
        return new PostResponse(
                post.getId(),
                post.getGroup().getId(),
                post.getGroup().getGroupName(),
                post.getAuthor().getId(),
                post.getAuthor().getUsername(),
                post.getBody(),
                post.getCommentsEnabled() != null ? post.getCommentsEnabled() : post.getGroup().isCommentsEnabled(),
                post.getCreatedAt()
        );
    }
}