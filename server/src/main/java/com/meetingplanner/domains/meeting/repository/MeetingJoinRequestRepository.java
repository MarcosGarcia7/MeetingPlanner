package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.common.api.model.RequestStatus;
import com.meetingplanner.domains.meeting.model.MeetingJoinRequestEntity;
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
public interface MeetingJoinRequestRepository extends JpaRepository<MeetingJoinRequestEntity, Long> {

    @Query("""
            select r from MeetingJoinRequestEntity r
            where r.meeting.id = :meetingId
              and (:status is null or r.status = :status)
              and not exists (
                  select 1 from FriendshipEntity f
                  where f.status = 'BLOCKED'
                    and ((f.user.id = :viewerId and f.friend.id = r.requester.id)
                         or (f.friend.id = :viewerId and f.user.id = r.requester.id)))
              and (cast(:cursorTime as OffsetDateTime) is null
                   or r.timeSent < :cursorTime
                   or (r.timeSent = :cursorTime and r.id < :cursorId))
            order by r.timeSent desc, r.id desc
            """)
    List<MeetingJoinRequestEntity> listForMeeting(@Param("meetingId") Long meetingId,
                                                  @Param("viewerId") Long viewerId,
                                                  @Param("status") RequestStatus status,
                                                  @Param("cursorTime") OffsetDateTime cursorTime,
                                                  @Param("cursorId") Long cursorId,
                                                  Pageable pageable);

    @Query("""
            select r from MeetingJoinRequestEntity r
            where r.requester.id = :requesterId
              and (:status is null or r.status = :status)
              and not exists (
                  select 1 from FriendshipEntity f
                  where f.status = 'BLOCKED'
                    and ((f.user.id = :requesterId and f.friend.id = r.meeting.creator.id)
                         or (f.friend.id = :requesterId and f.user.id = r.meeting.creator.id)))
              and (cast(:cursorTime as OffsetDateTime) is null
                   or r.timeSent < :cursorTime
                   or (r.timeSent = :cursorTime
                       and (:cursorKind > :sourceKind
                            or (:cursorKind = :sourceKind and r.id < :cursorId))))
            order by r.timeSent desc, r.id desc
            """)
    List<MeetingJoinRequestEntity> listForRequester(@Param("requesterId") Long requesterId,
                                                    @Param("status") RequestStatus status,
                                                    @Param("sourceKind") String sourceKind,
                                                    @Param("cursorTime") OffsetDateTime cursorTime,
                                                    @Param("cursorKind") String cursorKind,
                                                    @Param("cursorId") Long cursorId,
                                                    Pageable pageable);

    boolean existsByMeetingIdAndRequesterIdAndStatus(Long meetingId, Long requesterId, RequestStatus status);

    Optional<MeetingJoinRequestEntity> findByIdAndRequesterId(Long id, Long requesterId);

    @Query("""
            select r from MeetingJoinRequestEntity r
            where r.status = :status
              and ((r.requester.id = :a and r.meeting.creator.id = :b)
                   or (r.requester.id = :b and r.meeting.creator.id = :a))
            """)
    List<MeetingJoinRequestEntity> findBetweenRequesterAndCreator(@Param("a") Long a,
                                                                  @Param("b") Long b,
                                                                  @Param("status") RequestStatus status);

    @Modifying(flushAutomatically = true)
    @Query("""
            update MeetingJoinRequestEntity r
            set r.status = :to, r.respondedAt = :at
            where r.id = :id and r.status = :from
            """)
    int transitionStatus(@Param("id") Long id,
                         @Param("from") RequestStatus from,
                         @Param("to") RequestStatus to,
                         @Param("at") OffsetDateTime at);

    @Modifying(flushAutomatically = true)
    @Query("""
            update MeetingJoinRequestEntity r
            set r.status = com.meetingplanner.common.api.model.RequestStatus.CANCELLED, r.respondedAt = :at
            where r.requester.id = :userId
              and r.status = com.meetingplanner.common.api.model.RequestStatus.PENDING
              and r.meeting.id in (select m.id from MeetingEntity m where m.group.id = :groupId)
            """)
    int cancelPendingForRequesterInGroupMeetings(@Param("groupId") Long groupId,
                                                 @Param("userId") Long userId,
                                                 @Param("at") OffsetDateTime at);
}
