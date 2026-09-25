package com.meetingplanner.common.api.dto;

import com.meetingplanner.domains.friend.model.FriendshipEntity;
import com.meetingplanner.domains.friend.model.FriendshipStatus;

import java.time.OffsetDateTime;

public record FriendshipResponse(
        Long userId,
        String username,
        String firstName,
        String lastName,
        FriendshipStatus status,
        Long requesterId,
        OffsetDateTime createdAt
) {

    public static FriendshipResponse from(FriendshipEntity friendship, Long viewerId) {
        boolean requester = friendship.getRequester().getId().equals(viewerId);
        return new FriendshipResponse(
                requester ? friendship.getFriend().getId() : friendship.getUser().getId(),
                requester ? friendship.getFriend().getUsername() : friendship.getUser().getUsername(),
                requester ? friendship.getFriend().getFirstName() : friendship.getUser().getFirstName(),
                requester ? friendship.getFriend().getLastName() : friendship.getUser().getLastName(),
                friendship.getStatus(),
                friendship.getRequester().getId(),
                friendship.getCreatedAt()
        );
    }
}