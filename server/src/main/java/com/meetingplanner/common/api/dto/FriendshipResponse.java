package com.meetingplanner.common.api.dto;

import com.meetingplanner.domains.friend.model.FriendshipEntity;
import com.meetingplanner.domains.friend.model.FriendshipStatus;
import com.meetingplanner.domains.user.model.UserEntity;

import java.time.OffsetDateTime;

public record FriendshipResponse(
        Long userId,
        String username,
        String firstName,
        String lastName,
        String avatarColor,
        FriendshipStatus status,
        Long requesterId,
        OffsetDateTime createdAt
) {

    public static FriendshipResponse from(FriendshipEntity friendship, Long viewerId) {
        // Rows store the pair in id order, so the counterpart is whichever side isn't the viewer.
        UserEntity other = friendship.getUser().getId().equals(viewerId)
                ? friendship.getFriend()
                : friendship.getUser();
        return new FriendshipResponse(
                other.getId(),
                other.getUsername(),
                other.getFirstName(),
                other.getLastName(),
                other.getAvatarColor(),
                friendship.getStatus(),
                friendship.getRequester().getId(),
                friendship.getCreatedAt()
        );
    }
}
