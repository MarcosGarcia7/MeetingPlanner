package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.domains.meeting.model.MeetingInviteEntity;
import com.meetingplanner.common.api.model.InviteStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
              and (:cursorTime is null
                   or mi.timeSent < :cursorTime
                   or (mi.timeSent = :cursorTime
                       and (:cursorKind > :sourceKind
                            or (:cursorKind = :sourceKind and mi.id < :cursorId))))
            order by mi.timeSent desc, mi.id desc
            """)
    List<MeetingInviteEntity> listForRecipient(@Param("userId") Long userId,
                                               @Param("email") String email,
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
}
