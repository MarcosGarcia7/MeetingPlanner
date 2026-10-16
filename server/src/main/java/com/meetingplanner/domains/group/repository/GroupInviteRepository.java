package com.meetingplanner.domains.group.repository;

import com.meetingplanner.common.api.model.InviteStatus;
import com.meetingplanner.domains.group.model.GroupInviteEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupInviteRepository extends JpaRepository<GroupInviteEntity, Long> {

    @Query("""
            select gi from GroupInviteEntity gi
            where gi.group.id = :groupId
              and (:cursorTime is null
                   or gi.timeSent < :cursorTime
                   or (gi.timeSent = :cursorTime and gi.id < :cursorId))
            order by gi.timeSent desc, gi.id desc
            """)
    List<GroupInviteEntity> listByGroup(@Param("groupId") Long groupId,
                                        @Param("cursorTime") OffsetDateTime cursorTime,
                                        @Param("cursorId") Long cursorId,
                                        Pageable pageable);

    @Query("""
            select gi from GroupInviteEntity gi
            where (gi.recipient.id = :userId or gi.recipientEmail = :email)
              and (:cursorTime is null
                   or gi.timeSent < :cursorTime
                   or (gi.timeSent = :cursorTime
                       and (:cursorKind > :sourceKind
                            or (:cursorKind = :sourceKind and gi.id < :cursorId))))
            order by gi.timeSent desc, gi.id desc
            """)
    List<GroupInviteEntity> listForRecipient(@Param("userId") Long userId,
                                             @Param("email") String email,
                                             @Param("sourceKind") String sourceKind,
                                             @Param("cursorTime") OffsetDateTime cursorTime,
                                             @Param("cursorKind") String cursorKind,
                                             @Param("cursorId") Long cursorId,
                                             Pageable pageable);

    List<GroupInviteEntity> findByRecipientId(Long recipientId);

    List<GroupInviteEntity> findByRecipientIdAndStatus(Long recipientId, InviteStatus status);

    List<GroupInviteEntity> findByRecipientEmail(String recipientEmail);

    List<GroupInviteEntity> findBySenderId(Long senderId);

    boolean existsByGroupIdAndRecipientId(Long groupId, Long recipientId);

    Optional<GroupInviteEntity> findByTokenHash(String tokenHash);

    Optional<GroupInviteEntity> findByGroupIdAndRecipientEmail(Long groupId, String recipientEmail);
}
