package com.meetingplanner.domains.group.model;


import com.meetingplanner.domains.user.model.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "group_members")
@Setter
@Getter
@NoArgsConstructor
public class GroupMember {

    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @OneToMany
    @JoinColumn(name = "user_id")
    private UserEntity user;
}
