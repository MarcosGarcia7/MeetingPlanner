package com.meetingplanner.common.api.dto;

import com.meetingplanner.domains.group.model.GroupPostCommentEntity;
import com.meetingplanner.domains.group.model.GroupPostEntity;

import java.time.OffsetDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        Long groupId,
        Long authorId,
        String authorUsername,
        String body,
        boolean commentsEnabled,
        OffsetDateTime createdAt,
        List<PostCommentResponse> comments
) {

    public static PostResponse from(GroupPostEntity post, List<GroupPostCommentEntity> comments) {
        return new PostResponse(
                post.getId(),
                post.getGroup().getId(),
                post.getAuthor().getId(),
                post.getAuthor().getUsername(),
                post.getBody(),
                post.getCommentsEnabled() != null ? post.getCommentsEnabled() : post.getGroup().isCommentsEnabled(),
                post.getCreatedAt(),
                comments.stream().map(PostCommentResponse::from).toList()
        );
    }
}