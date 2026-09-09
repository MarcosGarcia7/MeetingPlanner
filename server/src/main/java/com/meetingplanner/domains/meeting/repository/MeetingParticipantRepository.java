package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.domains.meeting.model.MeetingParticipantEntity;
import com.meetingplanner.domains.meeting.model.MeetingParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipantEntity, MeetingParticipantId> {

    List<MeetingParticipantEntity> findByMeetingId(Long meetingId);

    List<MeetingParticipantEntity> findByUserId(Long userId);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);
}
