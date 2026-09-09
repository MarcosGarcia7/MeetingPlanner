package com.meetingplanner.domains.meeting.repository;


import com.meetingplanner.domains.meeting.model.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, Long> {

}
