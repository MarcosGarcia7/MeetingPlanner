package com.meetingplanner.domains.meeting.model;

import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.user.model.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "meetings")
public class MeetingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = true)
    private GroupEntity group;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "location", length = 500, nullable = true)
    private String location;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "external_participant_names", columnDefinition = "varchar(255)[]")
    private List<String> externalParticipantNames = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private UserEntity creator;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MeetingParticipantEntity> participants = new HashSet<>();

    public MeetingEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GroupEntity getGroup() {
        return group;
    }

    public void setGroup(GroupEntity group) {
        this.group = group;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    // A meeting without an end time is over once it starts, matching COALESCE(endTime, startTime) in queries.
    public boolean hasEnded() {
        OffsetDateTime end = endTime != null ? endTime : startTime;
        return end.isBefore(OffsetDateTime.now());
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getExternalParticipantNames() {
        return externalParticipantNames;
    }

    public void setExternalParticipantNames(List<String> externalParticipantNames) {
        this.externalParticipantNames = externalParticipantNames;
    }

    public UserEntity getCreator() {
        return creator;
    }

    public void setCreator(UserEntity creator) {
        this.creator = creator;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Set<MeetingParticipantEntity> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<MeetingParticipantEntity> participants) {
        this.participants = participants;
    }

    public void addParticipant(UserEntity user) {
        MeetingParticipantEntity participant = new MeetingParticipantEntity();
        participant.setMeeting(this);
        participant.setUser(user);
        this.participants.add(participant);
    }

    public void removeParticipant(UserEntity user) {
        this.participants.removeIf(participant -> Objects.equals(user.getId(), participant.getUser().getId()));
    }

}