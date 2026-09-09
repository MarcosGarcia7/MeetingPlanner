package com.meetingplanner.domains.meeting.model;

import com.meetingplanner.domains.user.model.UserEntity;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "meeting_participants")
@IdClass(MeetingParticipantId.class)
public class MeetingParticipantEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private MeetingEntity meeting;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    public MeetingParticipantEntity() {
    }

    public MeetingEntity getMeeting() {
        return meeting;
    }

    public void setMeeting(MeetingEntity meeting) {
        this.meeting = meeting;
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
        if (!(o instanceof MeetingParticipantEntity that)) return false;
        return this.meeting != null && this.user != null
                && Objects.equals(this.meeting.getId(), that.meeting.getId())
                && Objects.equals(this.user.getId(), that.user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}