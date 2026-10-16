package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.common.api.model.RequestStatus;
import com.meetingplanner.domains.meeting.model.MeetingJoinRequestEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
              and (:cursorTime is null
                   or r.timeSent < :cursorTime
                   or (r.timeSent = :cursorTime and r.id < :cursorId))
            order by r.timeSent desc, r.id desc
            """)
    List<MeetingJoinRequestEntity> listForMeeting(@Param("meetingId") Long meetingId,
                                                  @Param("cursorTime") OffsetDateTime cursorTime,
                                                  @Param("cursorId") Long cursorId,
                                                  Pageable pageable);

    @Query("""
            select r from MeetingJoinRequestEntity r
            where r.requester.id = :requesterId
              and (:cursorTime is null
                   or r.timeSent < :cursorTime
                   or (r.timeSent = :cursorTime
                       and (:cursorKind > :sourceKind
                            or (:cursorKind = :sourceKind and r.id < :cursorId))))
            order by r.timeSent desc, r.id desc
            """)
    List<MeetingJoinRequestEntity> listForRequester(@Param("requesterId") Long requesterId,
                                                    @Param("sourceKind") String sourceKind,
                                                    @Param("cursorTime") OffsetDateTime cursorTime,
                                                    @Param("cursorKind") String cursorKind,
                                                    @Param("cursorId") Long cursorId,
                                                    Pageable pageable);

    boolean existsByMeetingIdAndRequesterIdAndStatus(Long meetingId, Long requesterId, RequestStatus status);

    Optional<MeetingJoinRequestEntity> findByIdAndRequesterId(Long id, Long requesterId);
}
