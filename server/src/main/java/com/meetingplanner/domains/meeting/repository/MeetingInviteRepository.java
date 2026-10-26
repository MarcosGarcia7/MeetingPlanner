package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.domains.meeting.model.MeetingInviteEntity;
import com.meetingplanner.common.api.model.InviteStatus;
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
public interface MeetingInviteRepository extends JpaRepository<MeetingInviteEntity, Long> {

    @Query("""
            select mi from MeetingInviteEntity mi
            where (mi.recipient.id = :userId or mi.recipientEmail = :email)
              and (:status is null or mi.status = :status)
              and not exists (
                  select 1 from FriendshipEntity f
                  where f.status = 'BLOCKED'
                    and ((f.user.id = :userId and f.friend.id = mi.sender.id)
                         or (f.friend.id = :userId and f.user.id = mi.sender.id)))
              and (cast(:cursorTime as OffsetDateTime) is null
                   or mi.timeSent < :cursorTime
                   or (mi.timeSent = :cursorTime
                       and (:cursorKind > :sourceKind
                            or (:cursorKind = :sourceKind and mi.id < :cursorId))))
            order by mi.timeSent desc, mi.id desc
            """)
    List<MeetingInviteEntity> listForRecipient(@Param("userId") Long userId,
                                               @Param("email") String email,
                                               @Param("status") InviteStatus status,
                                               @Param("sourceKind") String sourceKind,
                                               @Param("cursorTime") OffsetDateTime cursorTime,
                                               @Param("cursorKind") String cursorKind,
                                               @Param("cursorId") Long cursorId,
                                               Pageable pageable);

    List<MeetingInviteEntity> findByRecipientId(Long recipientId);

    List<MeetingInviteEntity> findByRecipientIdAndStatus(Long recipientId, InviteStatus status);

    List<MeetingInviteEntity> findByRecipientEmail(String recipientEmail);

    List<MeetingInviteEntity> findBySenderId(Long senderId);

    boolean existsByMeetingIdAndRecipientId(Long meetingId, Long recipientId);

    Optional<MeetingInviteEntity> findByTokenHash(String tokenHash);

    Optional<MeetingInviteEntity> findByMeetingIdAndRecipientEmail(Long meetingId, String recipientEmail);

    @Query("""
            select mi from MeetingInviteEntity mi
            where mi.status = :status
              and ((mi.sender.id = :a and mi.recipient.id = :b)
                   or (mi.sender.id = :b and mi.recipient.id = :a))
            """)
    List<MeetingInviteEntity> findBetween(@Param("a") Long a,
                                          @Param("b") Long b,
                                          @Param("status") InviteStatus status);

    @Modifying(flushAutomatically = true)
    @Query("""
            update MeetingInviteEntity mi
            set mi.status = :to, mi.respondedAt = :at
            where mi.id = :id and mi.status = :from
            """)
    int transitionStatus(@Param("id") Long id,
                         @Param("from") InviteStatus from,
                         @Param("to") InviteStatus to,
                         @Param("at") OffsetDateTime at);

    @Modifying(flushAutomatically = true)
    @Query("""
            update MeetingInviteEntity mi
            set mi.status = com.meetingplanner.common.api.model.InviteStatus.DECLINED, mi.respondedAt = :at
            where mi.recipient.id = :userId
              and mi.status = com.meetingplanner.common.api.model.InviteStatus.PENDING
              and mi.meeting.id in (select m.id from MeetingEntity m where m.group.id = :groupId)
            """)
    int declinePendingForRecipientInGroupMeetings(@Param("groupId") Long groupId,
                                                  @Param("userId") Long userId,
                                                  @Param("at") OffsetDateTime at);
}
