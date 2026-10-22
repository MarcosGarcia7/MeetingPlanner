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

    @Column(name = "group_name", nullable = false, unique = true, columnDefinition = "citext")
    private String groupName;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = true;

    @Column(name = "members_can_post", nullable = false)
    private boolean membersCanPost = true;

    @Column(name = "comments_enabled", nullable = false)
    private boolean commentsEnabled = true;

    @Column(name = "members_can_create", nullable = false)
    private boolean membersCanCreate = true;

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

    public boolean isMembersCanPost() {
        return membersCanPost;
    }

    public void setMembersCanPost(boolean membersCanPost) {
        this.membersCanPost = membersCanPost;
    }

    public boolean isCommentsEnabled() {
        return commentsEnabled;
    }

    public void setCommentsEnabled(boolean commentsEnabled) {
        this.commentsEnabled = commentsEnabled;
    }

    public boolean isMembersCanCreate() {
        return membersCanCreate;
    }

    public void setMembersCanCreate(boolean membersCanCreate) {
        this.membersCanCreate = membersCanCreate;
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