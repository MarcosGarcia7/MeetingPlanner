package com.meetingplanner.domains.meeting.model;

import com.meetingplanner.domains.user.model.UserEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "meeting_reminders_sent")
@IdClass(MeetingReminderSentId.class)
public class MeetingReminderSentEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private MeetingEntity meeting;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "reminded_at", nullable = false, updatable = false)
    private OffsetDateTime remindedAt = OffsetDateTime.now();

    public MeetingReminderSentEntity() {
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

    public OffsetDateTime getRemindedAt() {
        return remindedAt;
    }

    public void setRemindedAt(OffsetDateTime remindedAt) {
        this.remindedAt = remindedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetingReminderSentEntity that)) return false;
        return this.meeting != null && this.user != null
                && Objects.equals(this.meeting.getId(), that.meeting.getId())
                && Objects.equals(this.user.getId(), that.user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}