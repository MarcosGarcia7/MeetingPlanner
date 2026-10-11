package com.meetingplanner.domains.meeting.repository;


import com.meetingplanner.domains.meeting.model.MeetingEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, Long> {

    List<MeetingEntity> findByStartTimeBetween(OffsetDateTime from, OffsetDateTime to);

    List<MeetingEntity> findByParticipants_User_Id(Long userId);

    List<MeetingEntity> findTop10ByParticipants_User_IdAndStartTimeAfterOrderByStartTimeAsc(Long userId, OffsetDateTime time);

    @Query("""
            select m from MeetingEntity m
            join m.participants p
            where p.user.id = :userId
              and (:groupId is null or m.group.id = :groupId)
              and (:cursorTime is null
                   or m.startTime < :cursorTime
                   or (m.startTime = :cursorTime and m.id < :cursorId))
            order by m.startTime desc, m.id desc
            """)
    List<MeetingEntity> listMine(@Param("userId") Long userId,
                                 @Param("groupId") Long groupId,
                                 @Param("cursorTime") OffsetDateTime cursorTime,
                                 @Param("cursorId") Long cursorId,
                                 Pageable pageable);

    @Query("""
            select m from MeetingEntity m
            where m.creator.id = :userId
              and (:cursorTime is null
                   or m.startTime < :cursorTime
                   or (m.startTime = :cursorTime and m.id < :cursorId))
            order by m.startTime desc, m.id desc
            """)
    List<MeetingEntity> listCreated(@Param("userId") Long userId,
                                    @Param("cursorTime") OffsetDateTime cursorTime,
                                    @Param("cursorId") Long cursorId,
                                    Pageable pageable);

    @Query("""
            select m from MeetingEntity m
            where m.group.id = :groupId
              and (m.isPrivate = false
                   or m.creator.id = :userId
                   or exists (select 1 from MeetingParticipantEntity p
                              where p.meeting = m and p.user.id = :userId))
              and (:cursorTime is null
                   or m.startTime < :cursorTime
                   or (m.startTime = :cursorTime and m.id < :cursorId))
            order by m.startTime desc, m.id desc
            """)
    List<MeetingEntity> listGroup(@Param("groupId") Long groupId,
                                  @Param("userId") Long userId,
                                  @Param("cursorTime") OffsetDateTime cursorTime,
                                  @Param("cursorId") Long cursorId,
                                  Pageable pageable);

    @Modifying
    @Query("DELETE FROM MeetingEntity m WHERE COALESCE(m.endTime, m.startTime) < :cutoff")
    int deleteMeetingsEndedBefore(@Param("cutoff") OffsetDateTime cutoff);
}
