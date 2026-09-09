package com.meetingplanner.domains.group.model;

import com.meetingplanner.domains.user.model.UserEntity;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "group_members")
@IdClass(GroupMemberId.class)
public class GroupMemberEntity {

    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity group;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    public GroupMemberEntity() {
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupMemberEntity that)) return false;
        return this.group != null && this.user != null
                && Objects.equals(this.group.getId(), that.group.getId())
                && Objects.equals(this.user.getId(), that.user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}