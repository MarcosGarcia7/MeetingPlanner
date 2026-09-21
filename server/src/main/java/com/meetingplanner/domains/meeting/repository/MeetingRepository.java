package com.meetingplanner.domains.meeting.repository;


import com.meetingplanner.domains.meeting.model.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, Long> {

    List<MeetingEntity> findByStartTimeBetween(OffsetDateTime from, OffsetDateTime to);

}
