package com.meetingplanner.domains.group.repository;

import com.meetingplanner.domains.group.model.GroupShareLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface GroupShareLinkRepository extends JpaRepository<GroupShareLinkEntity, Long> {

    Optional<GroupShareLinkEntity> findByToken(String token);

    Optional<GroupShareLinkEntity> findByGroupIdAndRevokedAtIsNull(Long groupId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update GroupShareLinkEntity l
            set l.revokedAt = :at
            where l.group.id = :groupId and l.revokedAt is null
            """)
    int revokeActive(@Param("groupId") Long groupId, @Param("at") OffsetDateTime at);

    // Conditional so concurrent joins can't push use_count past max_uses.
    @Modifying(flushAutomatically = true)
    @Query("""
            update GroupShareLinkEntity l
            set l.useCount = l.useCount + 1
            where l.id = :id
              and l.revokedAt is null
              and (l.maxUses is null or l.useCount < l.maxUses)
            """)
    int incrementUse(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("""
            delete from GroupShareLinkEntity l
            where l.revokedAt < :before or l.expiresAt < :before
            """)
    int deleteRevokedOrExpiredBefore(@Param("before") OffsetDateTime before);
}
