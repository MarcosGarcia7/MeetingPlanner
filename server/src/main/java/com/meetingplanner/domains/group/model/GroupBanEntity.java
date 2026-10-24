package com.meetingplanner.domains.group.model;

import com.meetingplanner.domains.user.model.UserEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "group_bans")
@IdClass(GroupBanId.class)
public class GroupBanEntity {

    @Column(name = "banned_by_owner", nullable = false)
    private boolean bannedByOwner;

    @Column(name = "banned_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private OffsetDateTime bannedAt = OffsetDateTime.now();

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity group;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banned_by_id", nullable = false)
    private UserEntity bannedBy;

    public GroupBanEntity() {
    }

    public boolean isBannedByOwner() {
        return bannedByOwner;
    }

    public void setBannedByOwner(boolean bannedByOwner) {
        this.bannedByOwner = bannedByOwner;
    }

    public OffsetDateTime getBannedAt() {
        return bannedAt;
    }

    public void setBannedAt(OffsetDateTime bannedAt) {
        this.bannedAt = bannedAt;
    }

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(GroupEntity group) {
        this.group = group;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public UserEntity getBannedBy() {
        return bannedBy;
    }

    public void setBannedBy(UserEntity bannedBy) {
        this.bannedBy = bannedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupBanEntity that)) return false;
        return this.group != null && this.user != null
                && Objects.equals(this.group.getId(), that.group.getId())
                && Objects.equals(this.user.getId(), that.user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
