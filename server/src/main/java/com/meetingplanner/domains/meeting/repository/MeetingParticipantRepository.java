package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.domains.meeting.model.MeetingParticipantEntity;
import com.meetingplanner.domains.meeting.model.MeetingParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipantEntity, MeetingParticipantId> {

    List<MeetingParticipantEntity> findByMeetingId(Long meetingId);

    List<MeetingParticipantEntity> findByUserId(Long userId);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    void deleteByMeetingIdAndUserId(Long meetingId, Long userId);

    @Query("select p.meeting.id, count(p) from MeetingParticipantEntity p " +
            "where p.meeting.id in :meetingIds group by p.meeting.id")
    List<Object[]> countByMeetingIds(@Param("meetingIds") Collection<Long> meetingIds);
}
