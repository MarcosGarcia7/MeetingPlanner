package com.meetingplanner.domains.meeting.repository;

import com.meetingplanner.domains.meeting.model.MeetingShareLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface MeetingShareLinkRepository extends JpaRepository<MeetingShareLinkEntity, Long> {

    Optional<MeetingShareLinkEntity> findByToken(String token);

    Optional<MeetingShareLinkEntity> findByMeetingIdAndRevokedAtIsNull(Long meetingId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update MeetingShareLinkEntity l
            set l.revokedAt = :at
            where l.meeting.id = :meetingId and l.revokedAt is null
            """)
    int revokeActive(@Param("meetingId") Long meetingId, @Param("at") OffsetDateTime at);

    // Conditional so concurrent joins can't push use_count past max_uses.
    @Modifying(flushAutomatically = true)
    @Query("""
            update MeetingShareLinkEntity l
            set l.useCount = l.useCount + 1
            where l.id = :id
              and l.revokedAt is null
              and (l.maxUses is null or l.useCount < l.maxUses)
            """)
    int incrementUse(@Param("id") Long id);
}
