package com.meetingplanner.domains.friend.model;

import java.io.Serializable;
import java.util.Objects;

public class FriendshipId implements Serializable {

    private Long user;
    private Long friend;

    public FriendshipId() {
    }

    public FriendshipId(Long userId, Long friendId) {
        this.user = userId;
        this.friend = friendId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FriendshipId that)) return false;
        return Objects.equals(user, that.user) && Objects.equals(friend, that.friend);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, friend);
    }
}