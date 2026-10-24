package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.domains.meeting.model.MeetingReminderSentEntity;
import com.meetingplanner.domains.meeting.model.MeetingReminderSentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface MeetingReminderSentRepository extends JpaRepository<MeetingReminderSentEntity, MeetingReminderSentId> {

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    // Returns 1 only for the caller that claimed the reminder, so concurrent workers never double-send.
    @Modifying
    @Query(value = """
            insert into meeting_reminders_sent (meeting_id, user_id, reminded_at)
            values (:meetingId, :userId, :at)
            on conflict do nothing
            """, nativeQuery = true)
    int claim(@Param("meetingId") Long meetingId,
              @Param("userId") Long userId,
              @Param("at") OffsetDateTime at);
}
