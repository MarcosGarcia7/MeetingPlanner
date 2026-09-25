package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.domains.meeting.model.MeetingReminderSentEntity;
import com.meetingplanner.domains.meeting.model.MeetingReminderSentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingReminderSentRepository extends JpaRepository<MeetingReminderSentEntity, MeetingReminderSentId> {

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);
}