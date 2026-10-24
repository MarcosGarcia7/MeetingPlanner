package com.meetingplanner.domains.group.repository;

import com.meetingplanner.common.api.model.RequestStatus;
import com.meetingplanner.domains.group.model.GroupJoinRequestEntity;
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
public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequestEntity, Long> {

    @Query("""
            select r from GroupJoinRequestEntity r
            where r.group.id = :groupId
              and (:cursorTime is null
                   or r.timeSent < :cursorTime
                   or (r.timeSent = :cursorTime and r.id < :cursorId))
            order by r.timeSent desc, r.id desc
            """)
    List<GroupJoinRequestEntity> listForGroup(@Param("groupId") Long groupId,
                                              @Param("cursorTime") OffsetDateTime cursorTime,
                                              @Param("cursorId") Long cursorId,
                                              Pageable pageable);

    @Query("""
            select r from GroupJoinRequestEntity r
            where r.requester.id = :requesterId
              and (:cursorTime is null
                   or r.timeSent < :cursorTime
                   or (r.timeSent = :cursorTime
                       and (:cursorKind > :sourceKind
                            or (:cursorKind = :sourceKind and r.id < :cursorId))))
            order by r.timeSent desc, r.id desc
            """)
    List<GroupJoinRequestEntity> listForRequester(@Param("requesterId") Long requesterId,
                                                  @Param("sourceKind") String sourceKind,
                                                  @Param("cursorTime") OffsetDateTime cursorTime,
                                                  @Param("cursorKind") String cursorKind,
                                                  @Param("cursorId") Long cursorId,
                                                  Pageable pageable);

    boolean existsByGroupIdAndRequesterIdAndStatus(Long groupId, Long requesterId, RequestStatus status);

    Optional<GroupJoinRequestEntity> findByIdAndRequesterId(Long id, Long requesterId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update GroupJoinRequestEntity r
            set r.status = :to, r.respondedAt = :at
            where r.id = :id and r.status = :from
            """)
    int transitionStatus(@Param("id") Long id,
                         @Param("from") RequestStatus from,
                         @Param("to") RequestStatus to,
                         @Param("at") OffsetDateTime at);

    @Modifying(flushAutomatically = true)
    @Query("""
            update GroupJoinRequestEntity r
            set r.status = com.meetingplanner.common.api.model.RequestStatus.CANCELLED, r.respondedAt = :at
            where r.group.id = :groupId
              and r.requester.id = :userId
              and r.status = com.meetingplanner.common.api.model.RequestStatus.PENDING
            """)
    int cancelPendingForRequesterInGroup(@Param("groupId") Long groupId,
                                         @Param("userId") Long userId,
                                         @Param("at") OffsetDateTime at);
}
