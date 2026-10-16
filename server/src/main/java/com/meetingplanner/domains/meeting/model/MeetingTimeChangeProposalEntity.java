package com.meetingplanner.domains.meeting.model;

import com.meetingplanner.common.api.model.ProposalStatus;
import com.meetingplanner.domains.user.model.UserEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "meeting_time_change_proposals")
public class MeetingTimeChangeProposalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private MeetingEntity meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposed_by", nullable = false)
    private UserEntity proposedBy;

    @Column(name = "proposed_start_time", nullable = false)
    private OffsetDateTime proposedStartTime;

    @Column(name = "proposed_end_time")
    private OffsetDateTime proposedEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProposalStatus status = ProposalStatus.PENDING;

    @Column(name = "time_sent", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private OffsetDateTime timeSent = OffsetDateTime.now();

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    public MeetingTimeChangeProposalEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MeetingEntity getMeeting() {
        return meeting;
    }

    public void setMeeting(MeetingEntity meeting) {
        this.meeting = meeting;
    }

    public UserEntity getProposedBy() {
        return proposedBy;
    }

    public void setProposedBy(UserEntity proposedBy) {
        this.proposedBy = proposedBy;
    }

    public OffsetDateTime getProposedStartTime() {
        return proposedStartTime;
    }

    public void setProposedStartTime(OffsetDateTime proposedStartTime) {
        this.proposedStartTime = proposedStartTime;
    }

    public OffsetDateTime getProposedEndTime() {
        return proposedEndTime;
    }

    public void setProposedEndTime(OffsetDateTime proposedEndTime) {
        this.proposedEndTime = proposedEndTime;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public OffsetDateTime getTimeSent() {
        return timeSent;
    }

    public void setTimeSent(OffsetDateTime timeSent) {
        this.timeSent = timeSent;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetingTimeChangeProposalEntity that)) return false;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
