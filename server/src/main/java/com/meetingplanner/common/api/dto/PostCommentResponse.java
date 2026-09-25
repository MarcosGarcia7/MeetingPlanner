package com.meetingplanner.common.api.dto;

import com.meetingplanner.domains.group.model.GroupPostCommentEntity;

import java.time.OffsetDateTime;

public record PostCommentResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorUsername,
        String body,
        OffsetDateTime createdAt
) {

    public static PostCommentResponse from(GroupPostCommentEntity comment) {
        return new PostCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername(),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }
}