package com.meetingplanner.domains.meeting.model;

import java.io.Serializable;
import java.util.Objects;

public class MeetingReminderSentId implements Serializable {

    private Long meeting;
    private Long user;

    public MeetingReminderSentId() {
    }

    public MeetingReminderSentId(Long meetingId, Long userId) {
        this.meeting = meetingId;
        this.user = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetingReminderSentId that)) return false;
        return Objects.equals(meeting, that.meeting) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meeting, user);
    }
}