package com.meetingplanner.domains.group.model;

import com.meetingplanner.domains.user.model.UserEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "groups")
public class GroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "group_name", length = 50, nullable = false, unique = true)
    private String groupName;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupMemberEntity> members = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public Set<GroupMemberEntity> getMembers() {
        return members;
    }

    public void setMembers(Set<GroupMemberEntity> members) {
        this.members = members;
    }

    public void addMember(UserEntity user, boolean isAdmin) {
        GroupMemberEntity member = new GroupMemberEntity();
        member.setGroup(this);
        member.setUser(user);
        member.setAdmin(isAdmin);
        this.members.add(member);
    }

    public void removeMember(UserEntity user) {
        this.members.removeIf(member -> Objects.equals(user.getId(), member.getUser().getId()));
    }

}