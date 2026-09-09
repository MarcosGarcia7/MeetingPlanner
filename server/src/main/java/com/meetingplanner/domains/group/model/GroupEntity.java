package com.meetingplanner.domains.group.model;

import com.meetingplanner.domains.user.model.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "groups")
@Setter
@Getter
@NoArgsConstructor
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "group_name", length = 50, nullable = false, unique = true)
    private String group_name;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = true;

    @OneToMany
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;
}
