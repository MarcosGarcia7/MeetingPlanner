package com.meetingplanner.domains.group.model;

import java.io.Serializable;
import java.util.Objects;

public class GroupBanId implements Serializable {

    private Long group;
    private Long user;

    public GroupBanId() {
    }

    public GroupBanId(Long groupId, Long userId) {
        this.group = groupId;
        this.user = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupBanId that)) return false;
        return Objects.equals(group, that.group) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, user);
    }
}
