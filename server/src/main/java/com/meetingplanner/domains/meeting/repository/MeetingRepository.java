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
                   or :canSeePrivate = true
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
                                  @Param("canSeePrivate") boolean canSeePrivate,
                                  @Param("cursorTime") OffsetDateTime cursorTime,
                                  @Param("cursorId") Long cursorId,
                                  Pageable pageable);

    @Query("""
            select m from MeetingEntity m
            join m.participants p
            where p.user.id = :userId
              and (:publicOnly = false or m.isPrivate = false)
              and (:cursorTime is null
                   or m.startTime < :cursorTime
                   or (m.startTime = :cursorTime and m.id < :cursorId))
            order by m.startTime desc, m.id desc
            """)
    List<MeetingEntity> listForUser(@Param("userId") Long userId,
                                    @Param("publicOnly") boolean publicOnly,
                                    @Param("cursorTime") OffsetDateTime cursorTime,
                                    @Param("cursorId") Long cursorId,
                                    Pageable pageable);

    @Modifying
    @Query("DELETE FROM MeetingEntity m WHERE COALESCE(m.endTime, m.startTime) < :cutoff")
    int deleteMeetingsEndedBefore(@Param("cutoff") OffsetDateTime cutoff);

    @Query("""
            select m from MeetingEntity m
            where (m.creator.id = :userId
                   or exists (select 1 from MeetingParticipantEntity p
                              where p.meeting = m and p.user.id = :userId))
              and (:excludeMeetingId is null or m.id <> :excludeMeetingId)
              and m.startTime < :end
              and COALESCE(m.endTime, m.startTime) > :start
            order by m.startTime asc
            """)
    List<MeetingEntity> findScheduleConflicts(@Param("userId") Long userId,
                                              @Param("start") OffsetDateTime start,
                                              @Param("end") OffsetDateTime end,
                                              @Param("excludeMeetingId") Long excludeMeetingId);

    @Query("""
            select m from MeetingEntity m
            join m.participants p
            where p.user.id = :userId
              and (:publicOnly = false or m.isPrivate = false)
              and (:excludeMeetingId is null or m.id <> :excludeMeetingId)
              and m.startTime < :end
              and COALESCE(m.endTime, m.startTime) > :start
            order by m.startTime asc
            """)
    List<MeetingEntity> findScheduleConflictsForUser(@Param("userId") Long userId,
                                                     @Param("publicOnly") boolean publicOnly,
                                                     @Param("start") OffsetDateTime start,
                                                     @Param("end") OffsetDateTime end,
                                                     @Param("excludeMeetingId") Long excludeMeetingId);

    @Query("""
            select m from MeetingEntity m
            left join m.group g
            where (lower(m.title) like :pattern escape '!'
                   or lower(coalesce(m.location, '')) like :pattern escape '!')
              and (m.creator.id = :userId
                   or exists (select 1 from MeetingParticipantEntity p
                              where p.meeting = m and p.user.id = :userId)
                   or (g is not null
                       and ((m.isPrivate = false
                             and (g.isPrivate = false
                                  or g.owner.id = :userId
                                  or exists (select 1 from GroupMemberEntity gm
                                             where gm.group = g and gm.user.id = :userId)))
                            or (m.isPrivate = true
                                and (g.owner.id = :userId
                                     or exists (select 1 from GroupMemberEntity gm
                                                where gm.group = g and gm.user.id = :userId and gm.isAdmin = true))))))
              and (:cursorTime is null
                   or m.startTime < :cursorTime
                   or (m.startTime = :cursorTime and m.id < :cursorId))
            order by m.startTime desc, m.id desc
            """)
    List<MeetingEntity> searchVisible(@Param("userId") Long userId,
                                      @Param("pattern") String pattern,
                                      @Param("cursorTime") OffsetDateTime cursorTime,
                                      @Param("cursorId") Long cursorId,
                                      Pageable pageable);
}
