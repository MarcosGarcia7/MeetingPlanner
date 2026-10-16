package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.common.api.model.ProposalStatus;
import com.meetingplanner.domains.meeting.model.MeetingTimeChangeProposalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingTimeChangeProposalRepository extends JpaRepository<MeetingTimeChangeProposalEntity, Long> {

    Optional<MeetingTimeChangeProposalEntity> findByMeetingIdAndStatus(Long meetingId, ProposalStatus status);

    Optional<MeetingTimeChangeProposalEntity> findByIdAndMeetingId(Long id, Long meetingId);

    List<MeetingTimeChangeProposalEntity> findByStatusAndTimeSentBefore(ProposalStatus status, OffsetDateTime cutoff);
}
